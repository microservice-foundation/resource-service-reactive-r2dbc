package com.epam.training.microservicefoundation.resourceservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.epam.training.microservicefoundation.resourceservice.client.StorageServiceClient;
import com.epam.training.microservicefoundation.resourceservice.model.StorageDTO;
import com.epam.training.microservicefoundation.resourceservice.model.StorageType;
import com.epam.training.microservicefoundation.resourceservice.model.exception.StorageNotFoundException;
import com.epam.training.microservicefoundation.resourceservice.service.implementation.StorageManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class StorageManagerTest {
  @Mock
  private StorageServiceClient storageServiceClient;
  @InjectMocks
  private StorageManager storageManager;

  @ParameterizedTest
  @EnumSource(StorageType.class)
  void shouldGetStorageByType(StorageType type) {
    final StorageDTO storage = storage(type);
    when(storageServiceClient.getByType(type)).thenReturn(Flux.just(storage));
    assertStorage(storage, storageManager.getByType(type));
  }

  @ParameterizedTest
  @EnumSource(StorageType.class)
  void shouldReturnStorageNotFoundExceptionWhenGetStorageByType(StorageType type) {
    when(storageServiceClient.getByType(type)).thenReturn(Flux.empty());
    StepVerifier.create(storageManager.getByType(type))
        .expectError(StorageNotFoundException.class)
        .verify();
  }

  @ParameterizedTest
  @EnumSource(StorageType.class)
  void shouldGetById(StorageType type) {
    final long id = 123L;
    StorageDTO storage = storage(type);
    when(storageServiceClient.getById(id)).thenReturn(Mono.just(storage));
    assertStorage(storage, storageManager.getById(id));
  }

  @Test
  void shouldReturnEmptyWhenGetById() {
    final long id = 123L;
    when(storageServiceClient.getById(id)).thenReturn(Mono.empty());
    StepVerifier.create(storageManager.getById(id))
        .expectSubscription()
        .expectNextCount(0)
        .verifyComplete();
  }

  private StorageDTO storage(StorageType type) {
    final long id = 123L;
    return StorageDTO.builder().id(id).type(type).bucket(type == StorageType.PERMANENT ? "permanent-bucket" : "staging-bucket").path(
        "files/").build();
  }

  private void assertStorage(StorageDTO expectValue, Mono<StorageDTO> actualValue) {
    StepVerifier.create(actualValue)
        .assertNext(result -> {
          assertEquals(expectValue.getId(), result.getId());
          assertEquals(expectValue.getType(), result.getType());
          assertEquals(expectValue.getBucket(), result.getBucket());
          assertEquals(expectValue.getPath(), result.getPath());
        }).verifyComplete();
  }
}
