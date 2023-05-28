
package com.epam.training.microservicefoundation.resourceservice.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.training.microservicefoundation.resourceservice.model.Resource;
import com.epam.training.microservicefoundation.resourceservice.model.ResourceMapper;
import com.epam.training.microservicefoundation.resourceservice.model.ResourceRecord;
import com.epam.training.microservicefoundation.resourceservice.model.exception.DownloadFailedException;
import com.epam.training.microservicefoundation.resourceservice.model.exception.ResourceNotFoundException;
import com.epam.training.microservicefoundation.resourceservice.model.exception.UploadFailedException;
import com.epam.training.microservicefoundation.resourceservice.repository.CloudStorageRepository;
import com.epam.training.microservicefoundation.resourceservice.repository.ResourceRepository;
import com.epam.training.microservicefoundation.resourceservice.repository.MockFilePart;
import com.epam.training.microservicefoundation.resourceservice.service.implementation.KafkaManager;
import com.epam.training.microservicefoundation.resourceservice.service.implementation.ResourceServiceImpl;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
  private KafkaManager kafkaManager;
  private ResourceService service;

  @BeforeEach
  public void setup() {
    service = new ResourceServiceImpl(resourceRepository, storageRepository, mapper, kafkaManager);
  }

  @Test
  void shouldSaveSong() throws IOException {
    long id = 1L;
    String fileKey = UUID.randomUUID().toString();
    String fileName = "mpthreetest.mp3";
    Path path = Paths.get("src/test/resources/files/mpthreetest.mp3");

    when(storageRepository.upload(any())).thenReturn(Mono.just(fileKey));
    when(mapper.mapToRecord(any())).thenReturn(new ResourceRecord(id));
    when(resourceRepository.save(any())).thenReturn(Mono.just(new Resource.Builder(fileKey, fileName).id(id).build()));
    when(kafkaManager.publish(any())).thenReturn(Mono.just(new FakeSendResult<>(null, null, null)));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    FilePart filePart = new MockFilePart(fileName, Files.readAllBytes(path), headers);
    Mono<ResourceRecord> resourceRecordMono = service.save(Mono.just(filePart));

    StepVerifier.create(resourceRecordMono)
        .assertNext(result -> {
          assertEquals(id, result.getId());
        })
        .verifyComplete();

    verify(storageRepository, only()).upload(any());
    verify(mapper, only()).mapToRecord(any());
    verify(resourceRepository, only()).save(any());
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenSaveEmptyMono() {
    Mono<ResourceRecord> resourceRecordMono = service.save(Mono.empty());

    StepVerifier.create(resourceRecordMono)
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void shouldThrowUploadFailedExceptionWhenSaveFilePart() throws IOException {
    String fileName = "mpthreetest.mp3";
    Path path = Paths.get("src/test/resources/files/mpthreetest.mp3");
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    FilePart filePart = new MockFilePart(fileName, Files.readAllBytes(path), headers);

    when(storageRepository.upload(filePart)).thenThrow(UploadFailedException.class);
    Mono<ResourceRecord> resourceRecordMono = service.save(Mono.just(filePart));

    StepVerifier.create(resourceRecordMono)
        .expectError(UploadFailedException.class)
        .verify();

    verify(storageRepository, only()).upload(filePart);
  }

  @Test
  void shouldThrowDataIntegrityViolationExceptionWhenSaveFilePart() throws IOException {
    String fileName = "mpthreetest.mp3";
    Path path = Paths.get("src/test/resources/files/mpthreetest.mp3");
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    FilePart filePart = new MockFilePart(fileName, Files.readAllBytes(path), headers);
    String fileKey = UUID.randomUUID().toString();
    Resource resource = new Resource.Builder(fileKey, fileName).build();

    when(storageRepository.upload(filePart)).thenReturn(Mono.just(fileKey));
    when(resourceRepository.save(resource)).thenThrow(DataIntegrityViolationException.class);
    Mono<ResourceRecord> resourceRecordMono = service.save(Mono.just(filePart));

    StepVerifier.create(resourceRecordMono)
        .expectError(DataIntegrityViolationException.class)
        .verify();

    verify(storageRepository, only()).upload(filePart);
    verify(resourceRepository, only()).save(resource);
  }

  @Test
  void shouldGetSong() throws IOException {
    long id = 1L;
    Path path = Paths.get("src/test/resources/files/mpthreetest.mp3");
    String fileKey = UUID.randomUUID().toString();
    when(resourceRepository.findById(id)).thenReturn(Mono.just(new Resource.Builder(fileKey,
        path.getFileName().toString()).build()));

    GetObjectResponse getObjectResponse = GetObjectResponse.builder().contentType(MediaType.MULTIPART_FORM_DATA.toString()).build();
    SdkPublisher<ByteBuffer> byteBufferSdkPublisher =
        SdkPublishers.envelopeWrappedPublisher(Mono.just(ByteBuffer.wrap(Files.readAllBytes(path))), "", "");
    when(storageRepository.getByFileKey(fileKey)).thenReturn(Mono.just(new ResponsePublisher<>(getObjectResponse, byteBufferSdkPublisher)));


    StepVerifier.create(service.getById(id))
        .assertNext(Assertions::assertNotNull)
        .verifyComplete();

    verify(resourceRepository, only()).findById(id);
    verify(storageRepository, only()).getByFileKey(fileKey);
  }

  @Test
  void shouldThrowNotFoundExceptionWhenGetById() {
    long id = 1L;
    when(resourceRepository.findById(id)).thenReturn(Mono.empty());

    StepVerifier.create(service.getById(id))
        .expectError(ResourceNotFoundException.class)
        .verify();

    verify(resourceRepository, only()).findById(id);
  }

  @Test
  void shouldThrowDownloadFailedExceptionWhenGetById() {
    long id = 1L;
    Path path = Paths.get("src/test/resources/files/mpthreetest.mp3");
    String fileKey = UUID.randomUUID().toString();
    when(resourceRepository.findById(id)).thenReturn(Mono.just(new Resource.Builder(fileKey,
        path.getFileName().toString()).build()));

    when(storageRepository.getByFileKey(fileKey)).thenThrow(DownloadFailedException.class);

    StepVerifier.create(service.getById(id))
        .expectError(DownloadFailedException.class)
        .verify();

    verify(resourceRepository, only()).findById(id);
    verify(storageRepository, only()).getByFileKey(fileKey);
  }

  @Test
  void shouldThrowNoSuchKeyExceptionWhenGetById() {
    long id = 119_900L;
    Path path = Paths.get("src/test/resources/files/mpthreetest.mp3");
    String fileKey = "123q";
    when(resourceRepository.findById(id)).thenReturn(Mono.just(new Resource.Builder(fileKey,
        path.getFileName().toString()).build()));

    when(storageRepository.getByFileKey(fileKey)).thenThrow(NoSuchKeyException.class);

    StepVerifier.create(service.getById(id))
        .expectError(NoSuchKeyException.class)
        .verify();
  }

  @Test
  void shouldDeleteResourceByIds() {
    List<Long> ids = List.of(1L, 2L);
    Resource resource1 = new Resource.Builder(UUID.randomUUID().toString(), "test1.mp3").id(ids.get(0)).build();
    when(resourceRepository.findById(ids.get(0))).thenReturn(Mono.just(resource1));
    Resource resource2 = new Resource.Builder(UUID.randomUUID().toString(), "test2.mp3").id(ids.get(1)).build();
    when(resourceRepository.findById(ids.get(1))).thenReturn(Mono.just(resource2));
    when(storageRepository.deleteByFileKey(resource1.getKey())).thenReturn(Mono.empty());
    when(storageRepository.deleteByFileKey(resource2.getKey())).thenReturn(Mono.empty());

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
  void shouldReturnEmptyWhenDeleteByIds() {
    List<Long> ids = List.of(1L, 2L);
    StepVerifier.create(service.deleteByIds(Flux.fromIterable(ids)))
        .expectSubscription()
        .expectNoEvent(Duration.ofMillis(500))
        .expectComplete();
  }

  @Test
  void shouldThrowDownloadFailedExceptionWhenDeleteResourceByIds() {
    List<Long> ids = List.of(1L, 2L);
    Resource resource = new Resource.Builder(UUID.randomUUID().toString(), "test1.mp3").id(ids.get(0)).build();
    when(resourceRepository.findById(ids.get(0))).thenReturn(Mono.just(resource));
    when(storageRepository.deleteByFileKey(resource.getKey())).thenThrow(DownloadFailedException.class);

    StepVerifier.create(service.deleteByIds(Flux.fromIterable(ids)))
        .expectError(DownloadFailedException.class)
        .verify();

    verify(resourceRepository, only()).findById(ids.get(0));
    verify(storageRepository, only()).deleteByFileKey(resource.getKey());
  }

  static class FakeSendResult<T> implements SenderResult<T> {
    private final RecordMetadata recordMetadata;
    private final Exception exception;
    private final T correlationMetadata;

    public FakeSendResult(RecordMetadata recordMetadata, Exception exception, T correlationMetadata) {
      this.recordMetadata = recordMetadata;
      this.exception = exception;
      this.correlationMetadata = correlationMetadata;
    }

    @Override
    public RecordMetadata recordMetadata() {
      return recordMetadata;
    }

    @Override
    public Exception exception() {
      return exception;
    }

    @Override
    public T correlationMetadata() {
      return correlationMetadata;
    }
  }
}

