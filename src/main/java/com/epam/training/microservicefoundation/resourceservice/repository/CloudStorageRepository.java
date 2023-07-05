package com.epam.training.microservicefoundation.resourceservice.repository;

import com.epam.training.microservicefoundation.resourceservice.config.properties.S3ClientConfigurationProperties;
import com.epam.training.microservicefoundation.resourceservice.model.ResourceFile;
import com.epam.training.microservicefoundation.resourceservice.model.StorageDTO;
import com.epam.training.microservicefoundation.resourceservice.model.UploadState;
import com.epam.training.microservicefoundation.resourceservice.model.exception.CopyObjectFailedException;
import com.epam.training.microservicefoundation.resourceservice.model.exception.DeleteFailedException;
import com.epam.training.microservicefoundation.resourceservice.model.exception.DownloadFailedException;
import com.epam.training.microservicefoundation.resourceservice.model.exception.UploadFailedException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;


public class CloudStorageRepository {
  private static final Logger log = LoggerFactory.getLogger(CloudStorageRepository.class);
  private final S3ClientConfigurationProperties properties;
  private final S3AsyncClient s3Client;
  private final Map<Class<? extends SdkResponse>, Function<SdkResponse, Mono<Void>>> checkExceptions;

  @Autowired
  public CloudStorageRepository(S3ClientConfigurationProperties properties, S3AsyncClient s3Client) {
    this.properties = properties;
    this.s3Client = s3Client;

    checkExceptions = new HashMap<>();
    checkExceptions.put(DeleteObjectResponse.class, response -> Mono.error(new DeleteFailedException(response)));
    checkExceptions.put(GetObjectResponse.class, response -> Mono.error(new DownloadFailedException(response)));
    checkExceptions.put(CreateMultipartUploadResponse.class, response -> Mono.error(new UploadFailedException(response)));
    checkExceptions.put(CopyObjectResponse.class, response -> Mono.error(new CopyObjectFailedException(response)));
  }

  public Mono<ResourceFile> upload(ResourceFile file) {
    log.info("Uploading file '{}' to storage '{}'", file.getFilePart().filename(), file.getStorage());
    return saveFile(file);
  }

  private Mono<ResourceFile> saveFile(ResourceFile file) {
    final String rawKey = UUID.randomUUID().toString();
    final FilePart filePart = file.getFilePart();
    final String filename = StringUtils.hasText(filePart.filename()) ? filePart.filename() : rawKey;

    final Map<String, String> metadata = new HashMap<>();
    metadata.put("filename", filename);

    final MediaType mediaType = Objects.isNull(filePart.headers().getContentType()) ? MediaType.APPLICATION_OCTET_STREAM :
       filePart.headers().getContentType();

    final String key = file.getStorage().getPath() + rawKey;
    final String bucket = file.getStorage().getBucket();
    final UploadState uploadState = new UploadState(bucket, key);
    CompletableFuture<CreateMultipartUploadResponse> uploadRequest =
        s3Client.createMultipartUpload(CreateMultipartUploadRequest.builder()
            .contentType(mediaType.toString())
            .key(key)
            .metadata(metadata)
            .bucket(bucket)
            .build());

    log.info("SaveFile: filekey={}, filename={}", key, filename);

    return Mono.fromFuture(uploadRequest)
        .flatMapMany(response -> {
          checkResult(response);
          uploadState.uploadId = response.uploadId();
          return filePart.content();
        })
        .bufferUntil(buffer -> {
          uploadState.buffered += buffer.readableByteCount();
          if (uploadState.buffered >= this.properties.getMultipartMinPartSize()) {
            log.debug("BufferUntil: returning true, bufferedBytes={}, partCounter={}, uploadId={}", uploadState.buffered,
                uploadState.partCounter, uploadState.uploadId);

            uploadState.buffered = 0;
            return true;
          }
          return false;
        })
        .map(CloudStorageRepository::concatBuffers)
        .flatMap(buffer -> uploadPart(uploadState, buffer))
        .onBackpressureBuffer()
        .reduce(uploadState, (state, completedPart) -> {
          log.debug("Completed: partNumber={}, etag={}", completedPart.partNumber(), completedPart.eTag());
          state.completedParts.put(completedPart.partNumber(), completedPart);
          return state;
        })
        .flatMap(this::completeUpload)
        .flatMap(response -> {
          log.debug("Saving file '{}' result {} to {} bucket ", filename, response, bucket);
          return checkResult(response).thenReturn(file.withFilename(filename).withKey(uploadState.filekey));
        });
  }

  private Mono<CompletedPart> uploadPart(UploadState uploadState, ByteBuffer buffer) {
    final int partNumber = ++uploadState.partCounter;
    log.info("UploadPart: partNumber={}, contentLength={}", partNumber, buffer.capacity());

    CompletableFuture<UploadPartResponse> request = s3Client.uploadPart(UploadPartRequest.builder()
            .bucket(uploadState.bucket)
            .key(uploadState.filekey)
            .partNumber(partNumber)
            .uploadId(uploadState.uploadId)
            .contentLength((long) buffer.capacity())
            .build(),
        AsyncRequestBody.fromPublisher(Mono.just(buffer)));

    return Mono
        .fromFuture(request)
        .map(uploadPartResult -> {
          checkResult(uploadPartResult);
          log.debug("UploadPart complete: part={}, etag={}", partNumber, uploadPartResult.eTag());
          return CompletedPart.builder()
              .eTag(uploadPartResult.eTag())
              .partNumber(partNumber)
              .build();
        });
  }

  private Mono<CompleteMultipartUploadResponse> completeUpload(UploadState state) {
    log.info("CompleteUpload: bucket={}, filekey={}, completedParts.size={}", state.bucket, state.filekey, state.completedParts.size());

    CompletedMultipartUpload multipartUpload = CompletedMultipartUpload.builder()
        .parts(state.completedParts.values())
        .build();

    return Mono.fromFuture(s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
        .bucket(state.bucket)
        .uploadId(state.uploadId)
        .multipartUpload(multipartUpload)
        .key(state.filekey)
        .build()));
  }

  private static ByteBuffer concatBuffers(List<DataBuffer> buffers) {
    log.info("Creating BytBuffer from {} chunks", buffers.size());

    int partSize = 0;
    for (DataBuffer b : buffers) {
      partSize += b.readableByteCount();
    }

    ByteBuffer partData = ByteBuffer.allocate(partSize);
    buffers.forEach(buffer -> partData.put(buffer.asByteBuffer()));

    // Reset read pointer to first byte
    partData.rewind();

    log.debug("PartData: size={}", partData.capacity());
    return partData;

  }

  public Mono<ResponsePublisher<GetObjectResponse>> getByKey(String key, String bucket) {
    log.info("Getting song by key '{}' from bucket '{}'", key, bucket);
    GetObjectRequest request = GetObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build();
    return Mono.fromFuture(s3Client.getObject(request, AsyncResponseTransformer.toPublisher()))
        .flatMap(response -> {
          log.debug("Getting song file result '{}' from bucket '{}'", response, bucket);
          return checkResult(response.response()).thenReturn(response);
        });
  }

  public Mono<Void> deleteByKey(String key, String bucket) {
    log.info("Deleting a song file by key '{}' from bucket '{}'", key, bucket);
    DeleteObjectRequest request = DeleteObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build();

    return Mono.fromFuture(s3Client.deleteObject(request)).flatMap(response -> {
      log.debug("Song file deletion result {} from bucket {}", response, bucket);
      return checkResult(response);
    });
  }
  public Mono<String> move(String key, StorageDTO fromStorage, StorageDTO toStorage) {
    log.info("Moving a resource file with key='{}' from source '{}' to destination '{}'", key, fromStorage, toStorage);
    final String destinationKey = replaceKeyPath(key, fromStorage.getPath(), toStorage.getPath());

    final CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
        .sourceBucket(fromStorage.getBucket())
        .sourceKey(key)
        .destinationBucket(toStorage.getBucket())
        .destinationKey(destinationKey)
        .build();

    return Mono.fromFuture(s3Client.copyObject(copyObjectRequest))
        .flatMap(response -> {
          log.debug("Copy file result {} from bucket '{}' to bucket '{}'", response, fromStorage.getBucket(),
              toStorage.getBucket());
          return checkResult(response);
        })
        .doOnSuccess(v -> deleteByKey(key, fromStorage.getBucket()))
        .thenReturn(destinationKey);
  }

  private String replaceKeyPath(String key, String originalPath, String newPath) {
    return key.replaceFirst(originalPath, newPath);
  }

  private Mono<Void> checkResult(SdkResponse response) {
    SdkHttpResponse sdkResponse = response.sdkHttpResponse();
    if (sdkResponse != null && sdkResponse.isSuccessful()) {
      return Mono.empty();
    }
    return checkExceptions.get(response.getClass()).apply(response);
  }
}
