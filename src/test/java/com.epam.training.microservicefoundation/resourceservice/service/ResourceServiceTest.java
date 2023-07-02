
package com.epam.training.microservicefoundation.resourceservice.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.epam.training.microservicefoundation.resourceservice.kafka.producer.KafkaProducer;
import com.epam.training.microservicefoundation.resourceservice.model.Resource;
import com.epam.training.microservicefoundation.resourceservice.model.ResourceDTO;
import com.epam.training.microservicefoundation.resourceservice.model.ResourceFile;
import com.epam.training.microservicefoundation.resourceservice.model.ResourceMapper;
import com.epam.training.microservicefoundation.resourceservice.model.ResourceProcessedEvent;
import com.epam.training.microservicefoundation.resourceservice.model.ResourceStatus;
import com.epam.training.microservicefoundation.resourceservice.model.StorageDTO;
import com.epam.training.microservicefoundation.resourceservice.model.StorageType;
import com.epam.training.microservicefoundation.resourceservice.model.exception.CopyObjectFailedException;
import com.epam.training.microservicefoundation.resourceservice.model.exception.DeleteFailedException;
import com.epam.training.microservicefoundation.resourceservice.model.exception.DownloadFailedException;
import com.epam.training.microservicefoundation.resourceservice.model.exception.ResourceNotFoundException;
import com.epam.training.microservicefoundation.resourceservice.model.exception.UploadFailedException;
import com.epam.training.microservicefoundation.resourceservice.repository.CloudStorageRepository;
import com.epam.training.microservicefoundation.resourceservice.repository.MockFilePart;
import com.epam.training.microservicefoundation.resourceservice.repository.ResourceRepository;
import com.epam.training.microservicefoundation.resourceservice.service.implementation.ResourceServiceImpl;
import com.epam.training.microservicefoundation.resourceservice.service.implementation.StorageManager;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.SenderResult;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.internal.async.SdkPublishers;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

  @Mock
  private ResourceRepository resourceRepository;
  @Mock
  private CloudStorageRepository storageRepository;
  @Mock
  private ResourceMapper mapper;
  @Mock
  private KafkaProducer kafkaProducer;
  @Mock
  private StorageManager storageManager;
  @InjectMocks
  private ResourceServiceImpl service;

  private static final String FILENAME = "mpthreetest.mp3";
  private static final Path FILE_PATH = Paths.get("src/test/resources/files/mpthreetest.mp3");
  private static final StorageDTO STAGING_STORAGE = StorageDTO.builder().type(StorageType.STAGING).path("files/")
      .bucket("resource-staging").build();
  private static final StorageDTO PERMANENT_STORAGE = StorageDTO.builder().type(StorageType.PERMANENT).path("files/")
      .bucket("resource-permanent").build();

  @Test
  void shouldSaveSong() throws IOException {
    final long id = 1L;
    final String fileKey = UUID.randomUUID().toString();
    final FilePart filePart = filePart();

    when(resourceRepository.existsByName(filePart.filename())).thenReturn(Mono.just(false));
    when(storageManager.getByType(StorageType.STAGING)).thenReturn(Mono.just(STAGING_STORAGE));
    when(storageRepository.upload(any(ResourceFile.class))).thenReturn(Mono.just(new ResourceFile(filePart, STAGING_STORAGE)
        .withKey(fileKey)));
    when(resourceRepository.save(any())).thenReturn(Mono.just(Resource.builder().name(filePart.filename()).key(fileKey)
        .status(ResourceStatus.STAGED).id(id).build()));
    when(mapper.mapToRecord(any())).thenReturn(new ResourceDTO(id));
    when(kafkaProducer.publish(any())).thenReturn(Mono.just(new FakeSenderResult<>(null, null, null)));

    Mono<ResourceDTO> resourceRecordMono = service.save(Mono.just(filePart));

    StepVerifier.create(resourceRecordMono)
        .assertNext(result -> {
          assertEquals(id, result.getId());
        })
        .verifyComplete();
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenSaveEmptyMono() {
    Mono<ResourceDTO> resourceRecordMono = service.save(Mono.empty());
    StepVerifier.create(resourceRecordMono)
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenSaveExistentResource() throws IOException {
    final FilePart filePart = filePart();
    when(resourceRepository.existsByName(filePart.filename())).thenReturn(Mono.just(true));
    Mono<ResourceDTO> resourceRecordMono = service.save(Mono.just(filePart));

    StepVerifier.create(resourceRecordMono)
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenSaveAndGetStorage() throws IOException {
    final FilePart filePart = filePart();
    when(resourceRepository.existsByName(filePart.filename())).thenReturn(Mono.just(false));
    when(storageManager.getByType(StorageType.STAGING)).thenReturn(Mono.empty());

    Mono<ResourceDTO> resourceRecordMono = service.save(Mono.just(filePart));

    StepVerifier.create(resourceRecordMono)
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void shouldThrowUploadFailedExceptionWhenSaveSong() throws IOException {
    final FilePart filePart = filePart();
    when(resourceRepository.existsByName(filePart.filename())).thenReturn(Mono.just(false));
    when(storageManager.getByType(StorageType.STAGING)).thenReturn(Mono.just(STAGING_STORAGE));
    when(storageRepository.upload(any(ResourceFile.class))).thenThrow(UploadFailedException.class);

    Mono<ResourceDTO> resourceRecordMono = service.save(Mono.just(filePart));

    StepVerifier.create(resourceRecordMono)
        .expectError(UploadFailedException.class)
        .verify();
  }

  @Test
  void shouldThrowDataIntegrityViolationExceptionWhenSaveSong() throws IOException {
    final String fileKey = UUID.randomUUID().toString();
    final FilePart filePart = filePart();

    when(resourceRepository.existsByName(filePart.filename())).thenReturn(Mono.just(false));
    when(storageManager.getByType(StorageType.STAGING)).thenReturn(Mono.just(STAGING_STORAGE));
    when(storageRepository.upload(any(ResourceFile.class))).thenReturn(Mono.just(new ResourceFile(filePart, STAGING_STORAGE)
        .withKey(fileKey)));
    when(resourceRepository.save(any())).thenThrow(DataIntegrityViolationException.class);

    Mono<ResourceDTO> resourceRecordMono = service.save(Mono.just(filePart));

    StepVerifier.create(resourceRecordMono)
        .expectError(DataIntegrityViolationException.class)
        .verify();
  }

  @Test
  void shouldThrowIllegalStateExceptionWhenSaveSong() throws IOException {
    final long id = 1L;
    final String fileKey = UUID.randomUUID().toString();
    final FilePart filePart = filePart();

    when(resourceRepository.existsByName(filePart.filename())).thenReturn(Mono.just(false));
    when(storageManager.getByType(StorageType.STAGING)).thenReturn(Mono.just(STAGING_STORAGE));
    when(storageRepository.upload(any(ResourceFile.class))).thenReturn(Mono.just(new ResourceFile(filePart, STAGING_STORAGE)
        .withKey(fileKey)));
    when(resourceRepository.save(any())).thenReturn(Mono.just(Resource.builder().name(filePart.filename()).key(fileKey)
        .status(ResourceStatus.STAGED).id(id).build()));
    when(kafkaProducer.publish(any())).thenThrow(IllegalStateException.class);

    Mono<ResourceDTO> resourceRecordMono = service.save(Mono.just(filePart));

    StepVerifier.create(resourceRecordMono)
        .expectError(IllegalStateException.class)
        .verify();
  }

  @Test
  void shouldGetSong() throws IOException {
    final long id = 1L;
    final String fileKey = UUID.randomUUID().toString();
    when(resourceRepository.findById(id)).thenReturn(Mono.just(Resource.builder().status(ResourceStatus.PROCESSED).key(fileKey).id(id)
        .build()));
    when(storageManager.getById(anyLong())).thenReturn(Mono.just(PERMANENT_STORAGE));
    final ResponsePublisher<GetObjectResponse> responsePublisher = getResponsePublisher();
    when(storageRepository.getByKey(fileKey, PERMANENT_STORAGE.getBucket())).thenReturn(Mono.just(responsePublisher));

    StepVerifier.create(service.getById(id))
        .assertNext(result -> {
          assertEquals(responsePublisher.response().contentType(), result.response().contentType());
        })
        .verifyComplete();
  }

  @Test
  void shouldThrowNotFoundExceptionWhenGetById() {
    final long id = 1L;
    final String fileKey = UUID.randomUUID().toString();
    when(resourceRepository.findById(id)).thenReturn(Mono.just(Resource.builder().status(ResourceStatus.PROCESSED).key(fileKey).id(id)
        .build()));
    when(storageManager.getById(anyLong())).thenReturn(Mono.empty());

    StepVerifier.create(service.getById(id))
        .expectError(ResourceNotFoundException.class)
        .verify();
  }

  @Test
  void shouldThrowDownloadFailedExceptionWhenGetById() {
    final long id = 1L;
    final String fileKey = UUID.randomUUID().toString();
    when(resourceRepository.findById(id)).thenReturn(Mono.just(Resource.builder().status(ResourceStatus.PROCESSED).key(fileKey).id(id)
        .build()));
    when(storageManager.getById(anyLong())).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(storageRepository.getByKey(fileKey, PERMANENT_STORAGE.getBucket())).thenThrow(DownloadFailedException.class);

    StepVerifier.create(service.getById(id))
        .expectError(DownloadFailedException.class)
        .verify();
  }

  @Test
  void shouldThrowNoSuchKeyExceptionWhenGetById() {
    final long id = 1L;
    final String fileKey = UUID.randomUUID().toString();
    when(resourceRepository.findById(id)).thenReturn(Mono.just(Resource.builder().status(ResourceStatus.PROCESSED).key(fileKey).id(id)
        .build()));
    when(storageManager.getById(anyLong())).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(storageRepository.getByKey(fileKey, PERMANENT_STORAGE.getBucket())).thenThrow(NoSuchKeyException.class);

    StepVerifier.create(service.getById(id))
        .expectError(NoSuchKeyException.class)
        .verify();
  }

  @Test
  void shouldDeleteResourceByIds() {
    final List<Long> ids = List.of(1L, 2L);
    final String key1 = UUID.randomUUID().toString();
    final String key2 = UUID.randomUUID().toString();
    final long storageId1 = 12L;
    final long storageId2 = 13L;
    final Resource resource1 =
        Resource.builder().name("test1.mp3").status(ResourceStatus.PROCESSED).key(key1).id(ids.get(0)).storageId(storageId1).build();
    when(resourceRepository.findById(ids.get(0))).thenReturn(Mono.just(resource1));
    Resource resource2 =
        Resource.builder().name("test2.mp3").status(ResourceStatus.PROCESSED).key(key2).id(ids.get(1)).storageId(storageId2).build();
    when(resourceRepository.findById(ids.get(1))).thenReturn(Mono.just(resource2));
    when(storageManager.getById(storageId1)).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(storageManager.getById(storageId2)).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(storageRepository.deleteByKey(key1, PERMANENT_STORAGE.getBucket())).thenReturn(Mono.empty());
    when(storageRepository.deleteByKey(key2, PERMANENT_STORAGE.getBucket())).thenReturn(Mono.empty());
    when(resourceRepository.delete(resource1)).thenReturn(Mono.empty());
    when(resourceRepository.delete(resource2)).thenReturn(Mono.empty());
    when(mapper.mapToRecord(resource1)).thenReturn(new ResourceDTO(ids.get(0)));
    when(mapper.mapToRecord(resource2)).thenReturn(new ResourceDTO(ids.get(1)));

    StepVerifier.create(service.deleteByIds(Flux.fromIterable(ids)))
        .assertNext(result -> {
          assertEquals(ids.get(0), result.getId());
        })
        .assertNext(result -> {
          assertEquals(ids.get(1), result.getId());
        })
        .verifyComplete();
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenDeleteByIds() {
    StepVerifier.create(service.deleteByIds(Flux.empty()))
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void shouldDeleteResourceByIdsEvenIfOneOfThemNotFound() {
    final List<Long> ids = List.of(1L, 2L);
    final String key2 = UUID.randomUUID().toString();
    final long storageId2 = 13L;
    when(resourceRepository.findById(ids.get(0))).thenReturn(Mono.empty());
    Resource resource2 =
        Resource.builder().name("test2.mp3").status(ResourceStatus.PROCESSED).key(key2).id(ids.get(1)).storageId(storageId2).build();
    when(resourceRepository.findById(ids.get(1))).thenReturn(Mono.just(resource2));
    when(storageManager.getById(storageId2)).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(storageRepository.deleteByKey(key2, PERMANENT_STORAGE.getBucket())).thenReturn(Mono.empty());
    when(resourceRepository.delete(resource2)).thenReturn(Mono.empty());
    when(mapper.mapToRecord(resource2)).thenReturn(new ResourceDTO(resource2.getId()));

    StepVerifier.create(service.deleteByIds(Flux.fromIterable(ids)))
        .assertNext(result -> {
          assertEquals(ids.get(1), result.getId());
        })
        .verifyComplete();
  }

  @Test
  void shouldThrowExceptionWhenDeleteByIds() {
    final List<Long> ids = List.of(1L, 2L);
    when(resourceRepository.findById(ids.get(0))).thenReturn(Mono.empty());
    when(resourceRepository.findById(ids.get(1))).thenReturn(Mono.empty());

    StepVerifier.create(service.deleteByIds(Flux.fromIterable(ids)))
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenDeleteByIdsAndGetStorageById() {
    final List<Long> ids = List.of(1L, 2L);
    final String key1 = UUID.randomUUID().toString();
    final String key2 = UUID.randomUUID().toString();
    final long storageId1 = 12L;
    final long storageId2 = 13L;
    final Resource resource1 =
        Resource.builder().name("test1.mp3").status(ResourceStatus.PROCESSED).key(key1).id(ids.get(0)).storageId(storageId1).build();
    when(resourceRepository.findById(ids.get(0))).thenReturn(Mono.just(resource1));
    Resource resource2 =
        Resource.builder().name("test2.mp3").status(ResourceStatus.PROCESSED).key(key2).id(ids.get(1)).storageId(storageId2).build();
    when(resourceRepository.findById(ids.get(1))).thenReturn(Mono.just(resource2));
    when(storageManager.getById(storageId1)).thenReturn(Mono.empty());
    when(storageManager.getById(storageId2)).thenReturn(Mono.empty());

    StepVerifier.create(service.deleteByIds(Flux.fromIterable(ids)))
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void shouldThrowDeleteFailedExceptionWhenDeleteByIdsFailAtOneOfThem() {
    final List<Long> ids = List.of(1L, 2L);
    final String key1 = UUID.randomUUID().toString();
    final String key2 = UUID.randomUUID().toString();
    final long storageId1 = 12L;
    final long storageId2 = 13L;
    final Resource resource1 =
        Resource.builder().name("test1.mp3").status(ResourceStatus.PROCESSED).key(key1).id(ids.get(0)).storageId(storageId1).build();
    when(resourceRepository.findById(ids.get(0))).thenReturn(Mono.just(resource1));
    Resource resource2 =
        Resource.builder().name("test2.mp3").status(ResourceStatus.PROCESSED).key(key2).id(ids.get(1)).storageId(storageId2).build();
    when(resourceRepository.findById(ids.get(1))).thenReturn(Mono.just(resource2));
    when(storageManager.getById(storageId1)).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(storageManager.getById(storageId2)).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(storageRepository.deleteByKey(key1, PERMANENT_STORAGE.getBucket())).thenReturn(Mono.empty());
    when(storageRepository.deleteByKey(key2, PERMANENT_STORAGE.getBucket())).thenThrow(DeleteFailedException.class);
    when(resourceRepository.delete(resource1)).thenReturn(Mono.empty());
    when(mapper.mapToRecord(resource1)).thenReturn(new ResourceDTO(resource1.getId()));

    StepVerifier.create(service.deleteByIds(Flux.fromIterable(ids)))
        .assertNext(result -> {
          assertEquals(ids.get(0), result.getId());
        })
        .expectError(DeleteFailedException.class)
        .verify();
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenDeleteByIdsDeleteResource() {
    final List<Long> ids = List.of(1L, 2L);
    final String key1 = UUID.randomUUID().toString();
    final String key2 = UUID.randomUUID().toString();
    final long storageId1 = 12L;
    final long storageId2 = 13L;
    final Resource resource1 =
        Resource.builder().name("test1.mp3").status(ResourceStatus.PROCESSED).key(key1).id(ids.get(0)).storageId(storageId1).build();
    when(resourceRepository.findById(ids.get(0))).thenReturn(Mono.just(resource1));
    Resource resource2 =
        Resource.builder().name("test2.mp3").status(ResourceStatus.PROCESSED).key(key2).id(ids.get(1)).storageId(storageId2).build();
    when(resourceRepository.findById(ids.get(1))).thenReturn(Mono.just(resource2));
    when(storageManager.getById(storageId1)).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(storageManager.getById(storageId2)).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(storageRepository.deleteByKey(key1, PERMANENT_STORAGE.getBucket())).thenReturn(Mono.empty());
    when(storageRepository.deleteByKey(key2, PERMANENT_STORAGE.getBucket())).thenReturn(Mono.empty());
    when(resourceRepository.delete(resource1)).thenReturn(Mono.empty());
    when(mapper.mapToRecord(resource1)).thenReturn(new ResourceDTO(resource1.getId()));
    when(resourceRepository.delete(resource2)).thenThrow(IllegalArgumentException.class);

    StepVerifier.create(service.deleteByIds(Flux.fromIterable(ids)))
        .assertNext(result -> {
          assertEquals(ids.get(0), result.getId());
        })
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void shouldMoveToPermanent() {
    final long resourceId = 1L;
    final String key = UUID.randomUUID().toString();
    final long storageId = 124L;
    when(resourceRepository.findById(resourceId)).thenReturn(Mono.just(
        Resource.builder().name("test1.mp3").status(ResourceStatus.STAGED).key(key).storageId(storageId).id(resourceId).build()));
    when(storageManager.getById(storageId)).thenReturn(Mono.just(STAGING_STORAGE));
    when(storageManager.getByType(any(StorageType.class))).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(storageRepository.move(anyString(), any(StorageDTO.class), any(StorageDTO.class)))
        .thenReturn(Mono.just(UUID.randomUUID().toString()));
    when(resourceRepository.save(any(Resource.class))).thenReturn(Mono.empty());

    StepVerifier.create(service.moveToPermanent(new ResourceProcessedEvent(resourceId)))
        .expectSubscription()
        .expectNextCount(0)
        .verifyComplete();
  }

  @Test
  void shouldDoNothingWhenMoveToPermanentAfterReturningEmptyWhenFindResourceById() {
    final long resourceId = 1L;
    when(resourceRepository.findById(resourceId)).thenReturn(Mono.empty());

    StepVerifier.create(service.moveToPermanent(new ResourceProcessedEvent(resourceId)))
        .expectSubscription()
        .expectNextCount(0)
        .verifyComplete();
  }

  @Test
  void shouldDoNothingWhenMoveToPermanentAfterReturningEmptyWhenGetStorageById() {
    final long resourceId = 1L;
    final String key = UUID.randomUUID().toString();
    final long storageId = 124L;
    when(resourceRepository.findById(resourceId)).thenReturn(Mono.just(
        Resource.builder().name("test1.mp3").status(ResourceStatus.STAGED).key(key).storageId(storageId).id(resourceId).build()));
    when(storageManager.getById(storageId)).thenReturn(Mono.empty());

    StepVerifier.create(service.moveToPermanent(new ResourceProcessedEvent(resourceId)))
        .expectSubscription()
        .expectNextCount(0)
        .verifyComplete();
  }

  @Test
  void shouldDoNothingWhenMoveToPermanentAfterReturningEmptyWhenGetStorageByType() {
    final long resourceId = 1L;
    final String key = UUID.randomUUID().toString();
    final long storageId = 124L;
    when(resourceRepository.findById(resourceId)).thenReturn(Mono.just(
        Resource.builder().name("test1.mp3").status(ResourceStatus.STAGED).key(key).storageId(storageId).id(resourceId).build()));
    when(storageManager.getById(storageId)).thenReturn(Mono.just(STAGING_STORAGE));
    when(storageManager.getByType(any(StorageType.class))).thenReturn(Mono.empty());

    StepVerifier.create(service.moveToPermanent(new ResourceProcessedEvent(resourceId)))
        .expectSubscription()
        .expectNextCount(0)
        .verifyComplete();
  }

  @Test
  void shouldCopyObjectFailedExceptionWhenMoveToPermanent() {
    final long resourceId = 1L;
    final String key = UUID.randomUUID().toString();
    final long storageId = 124L;
    when(resourceRepository.findById(resourceId)).thenReturn(Mono.just(
        Resource.builder().name("test1.mp3").status(ResourceStatus.STAGED).key(key).storageId(storageId).id(resourceId).build()));
    when(storageManager.getById(storageId)).thenReturn(Mono.just(STAGING_STORAGE));
    when(storageManager.getByType(any(StorageType.class))).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(storageRepository.move(anyString(), any(StorageDTO.class), any(StorageDTO.class)))
        .thenThrow(CopyObjectFailedException.class);

    StepVerifier.create(service.moveToPermanent(new ResourceProcessedEvent(resourceId)))
        .expectError(CopyObjectFailedException.class)
        .verify();
  }

  @Test
  void shouldDeleteFailedExceptionWhenMoveToPermanent() {
    final long resourceId = 1L;
    final String key = UUID.randomUUID().toString();
    final long storageId = 124L;
    when(resourceRepository.findById(resourceId)).thenReturn(Mono.just(
        Resource.builder().name("test1.mp3").status(ResourceStatus.STAGED).key(key).storageId(storageId).id(resourceId).build()));
    when(storageManager.getById(storageId)).thenReturn(Mono.just(STAGING_STORAGE));
    when(storageManager.getByType(any(StorageType.class))).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(storageRepository.move(anyString(), any(StorageDTO.class), any(StorageDTO.class)))
        .thenThrow(DeleteFailedException.class);

    StepVerifier.create(service.moveToPermanent(new ResourceProcessedEvent(resourceId)))
        .expectError(DeleteFailedException.class)
        .verify();
  }

  @Test
  void shouldIllegalArgumentExceptionWhenMoveToPermanent() {
    final long resourceId = 1L;
    final String key = UUID.randomUUID().toString();
    final long storageId = 124L;
    when(resourceRepository.findById(resourceId)).thenReturn(Mono.just(
        Resource.builder().name("test1.mp3").status(ResourceStatus.STAGED).key(key).storageId(storageId).id(resourceId).build()));
    when(storageManager.getById(storageId)).thenReturn(Mono.just(STAGING_STORAGE));
    when(storageManager.getByType(any(StorageType.class))).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(storageRepository.move(anyString(), any(StorageDTO.class), any(StorageDTO.class)))
        .thenReturn(Mono.just(UUID.randomUUID().toString()));
    when(resourceRepository.save(any(Resource.class))).thenThrow(IllegalArgumentException.class);

    StepVerifier.create(service.moveToPermanent(new ResourceProcessedEvent(resourceId)))
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  private ResponsePublisher<GetObjectResponse> getResponsePublisher() throws IOException {
    Path path = Paths.get("src/test/resources/files/mpthreetest.mp3");
    GetObjectResponse getObjectResponse = GetObjectResponse.builder().contentType(MediaType.MULTIPART_FORM_DATA.toString()).build();
    SdkPublisher<ByteBuffer> byteBufferSdkPublisher =
        SdkPublishers.envelopeWrappedPublisher(Mono.just(ByteBuffer.wrap(Files.readAllBytes(path))), "", "");
    return new ResponsePublisher<>(getObjectResponse, byteBufferSdkPublisher);
  }

  private FilePart filePart() throws IOException {
    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    return new MockFilePart(FILENAME, Files.readAllBytes(FILE_PATH), headers);
  }

  private static class FakeSenderResult<T> implements SenderResult<T> {
    private final RecordMetadata recordMetadata;
    private final Exception exception;
    private final T correlationMetadata;

    public FakeSenderResult(RecordMetadata recordMetadata, Exception exception, T correlationMetadata) {
      this.recordMetadata = recordMetadata;
      this.exception = exception;
      this.correlationMetadata = correlationMetadata;
    }

    @Override
    public RecordMetadata recordMetadata() {
      return this.recordMetadata;
    }

    @Override
    public Exception exception() {
      return this.exception;
    }

    @Override
    public T correlationMetadata() {
      return this.correlationMetadata;
    }
  }
}

