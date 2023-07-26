
package com.epam.training.microservicefoundation.resourceservice.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.epam.training.microservicefoundation.resourceservice.kafka.producer.KafkaProducer;
import com.epam.training.microservicefoundation.resourceservice.mapper.DeleteResourceMapper;
import com.epam.training.microservicefoundation.resourceservice.model.dto.DeleteResourceDTO;
import com.epam.training.microservicefoundation.resourceservice.model.dto.GetStorageDTO;
import com.epam.training.microservicefoundation.resourceservice.model.dto.StorageType;
import com.epam.training.microservicefoundation.resourceservice.model.entity.Resource;
import com.epam.training.microservicefoundation.resourceservice.model.entity.ResourceStatus;
import com.epam.training.microservicefoundation.resourceservice.model.exception.CloudStorageException;
import com.epam.training.microservicefoundation.resourceservice.model.exception.EntityNotFoundException;
import com.epam.training.microservicefoundation.resourceservice.repository.CloudStorageRepository;
import com.epam.training.microservicefoundation.resourceservice.repository.ResourceRepository;
import com.epam.training.microservicefoundation.resourceservice.service.implementation.BaseResourceServiceImpl;
import com.epam.training.microservicefoundation.resourceservice.service.implementation.StorageManager;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.internal.async.SdkPublishers;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@ExtendWith(MockitoExtension.class)
class BaseResourceServiceTest {

  @Mock
  private ResourceRepository resourceRepository;
  @Mock
  private CloudStorageRepository storageRepository;
  @Mock
  private DeleteResourceMapper deleteResourceMapper;
  @Mock
  private KafkaProducer kafkaProducer;
  @Mock
  private StorageManager storageManager;
  @InjectMocks
  private BaseResourceServiceImpl service;

  private static final GetStorageDTO PERMANENT_STORAGE = new GetStorageDTO(999L, "resource-permanent", "files/", StorageType.PERMANENT);

  @Test
  void shouldGetSong() throws IOException {
    final Resource savedResource = getSavedResource();
    when(resourceRepository.findById(savedResource.getId())).thenReturn(Mono.just(savedResource));
    when(storageManager.getById(anyLong())).thenReturn(Mono.just(PERMANENT_STORAGE));
    final ResponsePublisher<GetObjectResponse> responsePublisher = getResponsePublisher();
    when(storageRepository.getByKey(savedResource.getKey(), PERMANENT_STORAGE.getBucket())).thenReturn(Mono.just(responsePublisher));

    StepVerifier.create(service.getById(savedResource.getId()))
        .assertNext(result -> {
          assertEquals(responsePublisher.response().contentType(), result.response().contentType());
        })
        .verifyComplete();
  }

  @Test
  void shouldThrowNotFoundExceptionWhenGetById() {
    final Resource savedResource = getSavedResource();
    when(resourceRepository.findById(savedResource.getId())).thenReturn(Mono.just(savedResource));
    when(storageManager.getById(anyLong())).thenReturn(Mono.empty());

    StepVerifier.create(service.getById(savedResource.getId()))
        .expectError(EntityNotFoundException.class)
        .verify();
  }

  @Test
  void shouldThrowDownloadFailedExceptionWhenGetById() {
    final Resource savedResource = getSavedResource();
    when(resourceRepository.findById(savedResource.getId())).thenReturn(Mono.just(savedResource));
    when(storageManager.getById(anyLong())).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(storageRepository.getByKey(savedResource.getKey(), PERMANENT_STORAGE.getBucket())).thenThrow(CloudStorageException.class);

    StepVerifier.create(service.getById(savedResource.getId()))
        .expectError(CloudStorageException.class)
        .verify();
  }

  @Test
  void shouldThrowNoSuchKeyExceptionWhenGetById() {
    final Resource savedResource = getSavedResource();
    when(resourceRepository.findById(savedResource.getId())).thenReturn(Mono.just(savedResource));
    when(storageManager.getById(anyLong())).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(storageRepository.getByKey(savedResource.getKey(), PERMANENT_STORAGE.getBucket())).thenThrow(NoSuchKeyException.class);

    StepVerifier.create(service.getById(savedResource.getId()))
        .expectError(NoSuchKeyException.class)
        .verify();
  }

  @Test
  void shouldDeleteResourceByIds() {
    final Resource resource1 = getSavedResource();
    when(resourceRepository.findById(resource1.getId())).thenReturn(Mono.just(resource1));
    Resource resource2 = getSavedResource();
    when(resourceRepository.findById(resource2.getId())).thenReturn(Mono.just(resource2));
    when(storageManager.getById(resource1.getStorageId())).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(storageManager.getById(resource2.getStorageId())).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(storageRepository.deleteByKey(resource1.getKey(), PERMANENT_STORAGE.getBucket())).thenReturn(Mono.empty());
    when(storageRepository.deleteByKey(resource2.getKey(), PERMANENT_STORAGE.getBucket())).thenReturn(Mono.empty());
    when(resourceRepository.delete(resource1)).thenReturn(Mono.empty());
    when(resourceRepository.delete(resource2)).thenReturn(Mono.empty());
    when(deleteResourceMapper.toDto(resource1)).thenReturn(new DeleteResourceDTO(resource1.getId()));
    when(deleteResourceMapper.toDto(resource2)).thenReturn(new DeleteResourceDTO(resource2.getId()));

    StepVerifier.create(service.deleteByIds(new Long[]{resource1.getId(), resource2.getId()}))
        .assertNext(result -> {
          assertEquals(resource1.getId(), result.getId());
        })
        .assertNext(result -> {
          assertEquals(resource2.getId(), result.getId());
        })
        .verifyComplete();
  }

  @Test
  void shouldReturnNothingWhenDeleteByEmptyIds() {
    StepVerifier.create(service.deleteByIds(new Long[0]))
        .expectSubscription()
        .expectNextCount(0)
        .verifyComplete();
  }

  @Test
  void shouldDeleteResourceByIdsEvenIfOneOfThemNotFound() {
    final Resource resource1 = getSavedResource();
    when(resourceRepository.findById(resource1.getId())).thenReturn(Mono.empty());
    Resource resource2 = getSavedResource();
    when(resourceRepository.findById(resource2.getId())).thenReturn(Mono.just(resource2));
    when(storageManager.getById(resource2.getStorageId())).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(storageRepository.deleteByKey(resource2.getKey(), PERMANENT_STORAGE.getBucket())).thenReturn(Mono.empty());
    when(resourceRepository.delete(resource2)).thenReturn(Mono.empty());
    when(deleteResourceMapper.toDto(resource2)).thenReturn(new DeleteResourceDTO(resource2.getId()));

    StepVerifier.create(service.deleteByIds(new Long[] {resource1.getId(), resource2.getId()}))
        .assertNext(result -> {
          assertEquals(resource2.getId(), result.getId());
        })
        .verifyComplete();
  }

  @Test
  void shouldReturnNothingWhenDeleteNonExistentResourcesByIds() {
    final Long[] ids = {1L, 2L};
    when(resourceRepository.findById(ids[0])).thenReturn(Mono.empty());
    when(resourceRepository.findById(ids[1])).thenReturn(Mono.empty());

    StepVerifier.create(service.deleteByIds(ids))
        .expectSubscription()
        .expectNextCount(0)
        .verifyComplete();
  }

  @Test
  void shouldReturnNothingWhenDeleteByIdsAndCannotGetStorageById() {
    final Resource resource1 = getSavedResource();
    when(resourceRepository.findById(resource1.getId())).thenReturn(Mono.just(resource1));
    Resource resource2 = getSavedResource();
    when(resourceRepository.findById(resource2.getId())).thenReturn(Mono.just(resource2));
    when(storageManager.getById(resource1.getStorageId())).thenReturn(Mono.empty());
    when(storageManager.getById(resource2.getStorageId())).thenReturn(Mono.empty());

    StepVerifier.create(service.deleteByIds(new Long[] {resource1.getId(), resource2.getId()}))
        .expectSubscription()
        .expectNextCount(0)
        .verifyComplete();
  }

  @Test
  void shouldThrowDeleteFailedExceptionWhenDeleteByIdsFailAtOneOfThem() {
    final Resource resource1 = getSavedResource();
    when(resourceRepository.findById(resource1.getId())).thenReturn(Mono.just(resource1));
    Resource resource2 = getSavedResource();
    when(resourceRepository.findById(resource2.getId())).thenReturn(Mono.just(resource2));
    when(storageManager.getById(resource1.getStorageId())).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(storageManager.getById(resource2.getStorageId())).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(storageRepository.deleteByKey(resource1.getKey(), PERMANENT_STORAGE.getBucket())).thenReturn(Mono.empty());
    when(storageRepository.deleteByKey(resource2.getKey(), PERMANENT_STORAGE.getBucket())).thenThrow(CloudStorageException.class);
    when(resourceRepository.delete(resource1)).thenReturn(Mono.empty());
    when(deleteResourceMapper.toDto(resource1)).thenReturn(new DeleteResourceDTO(resource1.getId()));

    StepVerifier.create(service.deleteByIds(new Long[] {resource1.getId(), resource2.getId()}))
        .assertNext(result -> {
          assertEquals(resource1.getId(), result.getId());
        })
        .expectError(CloudStorageException.class)
        .verify();
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenDeleteByIdsDeleteResource() {
    final Resource resource1 = getSavedResource();
    when(resourceRepository.findById(resource1.getId())).thenReturn(Mono.just(resource1));
    final Resource resource2 = getSavedResource();
    when(resourceRepository.findById(resource2.getId())).thenReturn(Mono.just(resource2));
    when(storageManager.getById(resource1.getStorageId())).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(storageManager.getById(resource2.getStorageId())).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(storageRepository.deleteByKey(resource1.getKey(), PERMANENT_STORAGE.getBucket())).thenReturn(Mono.empty());
    when(storageRepository.deleteByKey(resource2.getKey(), PERMANENT_STORAGE.getBucket())).thenReturn(Mono.empty());
    when(resourceRepository.delete(resource1)).thenReturn(Mono.empty());
    when(deleteResourceMapper.toDto(resource1)).thenReturn(new DeleteResourceDTO(resource1.getId()));
    when(resourceRepository.delete(resource2)).thenThrow(IllegalArgumentException.class);

    StepVerifier.create(service.deleteByIds(new Long[] {resource1.getId(), resource2.getId()}))
        .assertNext(result -> {
          assertEquals(resource1.getId(), result.getId());
        })
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  private static final Random RANDOM = new Random();
  private Resource getSavedResource() {
    int id = RANDOM.nextInt(1000);
    return Resource.builder().status(ResourceStatus.PROCESSED).key(UUID.randomUUID().toString()).id(id)
        .storageId(RANDOM.nextInt(1000)).name("test" + id).build();
  }

  private ResponsePublisher<GetObjectResponse> getResponsePublisher() throws IOException {
    final Path path = Paths.get("src/test/resources/files/mpthreetest.mp3");
    final GetObjectResponse getObjectResponse = GetObjectResponse.builder().contentType(MediaType.MULTIPART_FORM_DATA.toString()).build();
    final SdkPublisher<ByteBuffer> byteBufferSdkPublisher =
        SdkPublishers.envelopeWrappedPublisher(Mono.just(ByteBuffer.wrap(Files.readAllBytes(path))), "", "");
    return new ResponsePublisher<>(getObjectResponse, byteBufferSdkPublisher);
  }

}

