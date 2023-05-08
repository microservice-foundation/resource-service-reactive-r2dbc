package com.epam.training.microservicefoundation.resourceservice.repository;

import com.epam.training.microservicefoundation.resourceservice.configuration.S3ClientConfigurationProperties;
import com.epam.training.microservicefoundation.resourceservice.model.UploadState;
import com.epam.training.microservicefoundation.resourceservice.model.exception.DeleteFailedException;
import com.epam.training.microservicefoundation.resourceservice.model.exception.DownloadFailedException;
import com.epam.training.microservicefoundation.resourceservice.model.exception.UploadFailedException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
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
    checkExceptions.put(CreateMultipartUploadResponse.class, response -> Mono.error( new UploadFailedException(response)));
  }

  public Mono<String> upload(FilePart filePart) {
    log.info("Uploading file '{}' to bucket '{}'", filePart.filename(), properties.getBucketName());
    return saveFile(filePart);
  }

  private Mono<String> saveFile(FilePart filePart) {
    String fileKey = UUID.randomUUID().toString();
    log.info("SaveFile: filekey={}, filename={}", fileKey, filePart.filename());
    String filename = filePart.filename() != null && !filePart.filename().isEmpty() ? filePart.filename() : fileKey;
    Map<String, String> metadata = new HashMap<>();
    metadata.put("filename", filename);

    MediaType mediaType = filePart.headers().getContentType() != null ? filePart.headers().getContentType() :
        MediaType.APPLICATION_OCTET_STREAM;

    UploadState uploadState = new UploadState(properties.getBucketName(), fileKey);
    CompletableFuture<CreateMultipartUploadResponse> uploadRequest =
        s3Client.createMultipartUpload(CreateMultipartUploadRequest.builder()
            .contentType(mediaType.toString())
            .key(fileKey)
            .metadata(metadata)
            .bucket(properties.getBucketName())
            .build());
    return Mono.fromFuture(uploadRequest)
        .flatMapMany(response -> {
          checkResult(response);
          uploadState.uploadId = response.uploadId();
          return filePart.content();
        })
        .bufferUntil(buffer -> {
          uploadState.buffered += buffer.readableByteCount();
          if (uploadState.buffered >= properties.getMultipartMinPartSize()) {
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
        .map(response -> {
          checkResult(response);
          return uploadState.filekey;
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

  public Mono<ResponsePublisher<GetObjectResponse>> getByFileKey(String fileKey) {
    log.info("Getting song by fileKey '{}' from bucket '{}'", fileKey, properties.getBucketName());
    GetObjectRequest request = GetObjectRequest.builder()
        .bucket(properties.getBucketName())
        .key(fileKey)
        .build();
    return Mono.fromFuture(s3Client.getObject(request, AsyncResponseTransformer.toPublisher()))
        .map(response -> {
          log.debug("Getting song file result '{}' from bucket '{}'", response, properties.getBucketName());
          checkResult(response.response());
          return response;
        });
  }

  public Mono<Void> deleteByFileKey(String fileKey) {
    log.info("Deleting a song file by key '{}' from bucket '{}'", fileKey, properties.getBucketName());
    DeleteObjectRequest request = DeleteObjectRequest.builder()
        .bucket(properties.getBucketName())
        .key(fileKey)
        .build();

    return Mono.fromFuture(s3Client.deleteObject(request)).flatMap(response -> {
      log.debug("Song file deletion result {} from bucket {}", response, properties.getBucketName());
      return checkResult(response);
    });
  }

  private Mono<Void> checkResult(SdkResponse response) {
    SdkHttpResponse sdkResponse = response.sdkHttpResponse();
    if (sdkResponse != null && sdkResponse.isSuccessful()) {
      return Mono.empty();
    }
    return checkExceptions.get(response.getClass()).apply(response);
  }
}
