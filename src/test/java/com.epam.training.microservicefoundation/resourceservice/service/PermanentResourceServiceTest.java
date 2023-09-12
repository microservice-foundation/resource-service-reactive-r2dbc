package com.epam.training.microservicefoundation.resourceservice.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.epam.training.microservicefoundation.resourceservice.domain.dto.GetStorageDTO;
import com.epam.training.microservicefoundation.resourceservice.domain.dto.StorageType;
import com.epam.training.microservicefoundation.resourceservice.domain.entity.Resource;
import com.epam.training.microservicefoundation.resourceservice.domain.entity.ResourceStatus;
import com.epam.training.microservicefoundation.resourceservice.domain.event.ResourceProcessedEvent;
import com.epam.training.microservicefoundation.resourceservice.domain.exception.CloudStorageException;
import com.epam.training.microservicefoundation.resourceservice.repository.CloudStorageRepository;
import com.epam.training.microservicefoundation.resourceservice.repository.ResourceRepository;
import com.epam.training.microservicefoundation.resourceservice.service.implementation.PermanentResourceService;
import com.epam.training.microservicefoundation.resourceservice.service.implementation.StorageManager;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class PermanentResourceServiceTest {
  @Mock
  private StorageManager storageManager;
  @Mock
  private ResourceRepository resourceRepository;
  @Mock
  private CloudStorageRepository cloudStorageRepository;
  @InjectMocks
  private PermanentResourceService permanentResourceService;

  private static final GetStorageDTO PERMANENT_STORAGE = new GetStorageDTO(999L, "resource-permanent", "files/", StorageType.PERMANENT);
  private static final GetStorageDTO STAGING_STORAGE = new GetStorageDTO(998L, "resource-staging", "files/", StorageType.STAGING);

  @Test
  void shouldMoveToPermanent() {
    final Resource savedResource = getSavedResource();
    when(resourceRepository.findById(savedResource.getId())).thenReturn(Mono.just(savedResource));
    when(storageManager.getById(savedResource.getStorageId())).thenReturn(Mono.just(STAGING_STORAGE));
    when(storageManager.getByType(any(StorageType.class))).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(cloudStorageRepository.move(anyString(), any(GetStorageDTO.class), any(GetStorageDTO.class)))
        .thenReturn(Mono.just(UUID.randomUUID().toString()));
    when(resourceRepository.save(any(Resource.class))).thenReturn(Mono.empty());

    StepVerifier.create(permanentResourceService.saveToPermanent(new ResourceProcessedEvent(savedResource.getId())))
        .expectSubscription()
        .expectNextCount(0)
        .verifyComplete();
  }

  @Test
  void shouldDoNothingWhenMoveToPermanentAfterReturningEmptyWhenFindResourceById() {
    final long resourceId = 1L;
    when(resourceRepository.findById(resourceId)).thenReturn(Mono.empty());

    StepVerifier.create(permanentResourceService.saveToPermanent(new ResourceProcessedEvent(resourceId)))
        .expectSubscription()
        .expectNextCount(0)
        .verifyComplete();
  }

  @Test
  void shouldDoNothingWhenMoveToPermanentAfterReturningEmptyWhenGetStorageById() {
    final Resource savedResource = getSavedResource();
    when(resourceRepository.findById(savedResource.getId())).thenReturn(Mono.just(savedResource));
    when(storageManager.getById(savedResource.getStorageId())).thenReturn(Mono.empty());

    StepVerifier.create(permanentResourceService.saveToPermanent(new ResourceProcessedEvent(savedResource.getId())))
        .expectSubscription()
        .expectNextCount(0)
        .verifyComplete();
  }

  @Test
  void shouldDoNothingWhenMoveToPermanentAfterReturningEmptyWhenGetStorageByType() {
    final Resource savedResource = getSavedResource();
    when(resourceRepository.findById(savedResource.getId())).thenReturn(Mono.just(savedResource));
    when(storageManager.getById(savedResource.getStorageId())).thenReturn(Mono.just(STAGING_STORAGE));
    when(storageManager.getByType(any(StorageType.class))).thenReturn(Mono.empty());

    StepVerifier.create(permanentResourceService.saveToPermanent(new ResourceProcessedEvent(savedResource.getId())))
        .expectSubscription()
        .expectNextCount(0)
        .verifyComplete();
  }

  @Test
  void shouldCopyObjectFailedExceptionWhenMoveToPermanent() {
    final Resource savedResource = getSavedResource();
    when(resourceRepository.findById(savedResource.getId())).thenReturn(Mono.just(savedResource));
    when(storageManager.getById(savedResource.getStorageId())).thenReturn(Mono.just(STAGING_STORAGE));
    when(storageManager.getByType(any(StorageType.class))).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(cloudStorageRepository.move(anyString(), any(GetStorageDTO.class), any(GetStorageDTO.class)))
        .thenThrow(CloudStorageException.class);

    StepVerifier.create(permanentResourceService.saveToPermanent(new ResourceProcessedEvent(savedResource.getId())))
        .expectError(CloudStorageException.class)
        .verify();
  }

  @Test
  void shouldDeleteFailedExceptionWhenMoveToPermanent() {
    final Resource savedResource = getSavedResource();
    when(resourceRepository.findById(savedResource.getId())).thenReturn(Mono.just(savedResource));
    when(storageManager.getById(savedResource.getStorageId())).thenReturn(Mono.just(STAGING_STORAGE));
    when(storageManager.getByType(any(StorageType.class))).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(cloudStorageRepository.move(anyString(), any(GetStorageDTO.class), any(GetStorageDTO.class)))
        .thenThrow(CloudStorageException.class);

    StepVerifier.create(permanentResourceService.saveToPermanent(new ResourceProcessedEvent(savedResource.getId())))
        .expectError(CloudStorageException.class)
        .verify();
  }

  @Test
  void shouldIllegalArgumentExceptionWhenMoveToPermanent() {
    final Resource savedResource = getSavedResource();
    when(resourceRepository.findById(savedResource.getId())).thenReturn(Mono.just(savedResource));
    when(storageManager.getById(savedResource.getStorageId())).thenReturn(Mono.just(STAGING_STORAGE));
    when(storageManager.getByType(any(StorageType.class))).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(cloudStorageRepository.move(anyString(), any(GetStorageDTO.class), any(GetStorageDTO.class)))
        .thenReturn(Mono.just(UUID.randomUUID().toString()));
    when(resourceRepository.save(any(Resource.class))).thenThrow(IllegalArgumentException.class);

    StepVerifier.create(permanentResourceService.saveToPermanent(new ResourceProcessedEvent(savedResource.getId())))
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  private static final Random RANDOM = new Random();

  private Resource getSavedResource() {
    int id = RANDOM.nextInt(1000);
    return Resource.builder().status(ResourceStatus.PROCESSED).key(UUID.randomUUID().toString()).id(id)
        .storageId(RANDOM.nextInt(1000)).name("test" + id).build();
  }
}
