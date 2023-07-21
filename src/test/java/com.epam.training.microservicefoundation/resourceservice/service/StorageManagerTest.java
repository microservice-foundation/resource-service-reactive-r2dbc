package com.epam.training.microservicefoundation.resourceservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.epam.training.microservicefoundation.resourceservice.client.StorageServiceClient;
import com.epam.training.microservicefoundation.resourceservice.model.dto.GetStorageDTO;
import com.epam.training.microservicefoundation.resourceservice.model.dto.StorageType;
import com.epam.training.microservicefoundation.resourceservice.model.exception.EntityNotFoundException;
import com.epam.training.microservicefoundation.resourceservice.service.implementation.StorageManager;
import java.util.Random;
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
    final GetStorageDTO storage = storage(type);
    when(storageServiceClient.getByType(type)).thenReturn(Flux.just(storage));
    assertStorage(storage, storageManager.getByType(type));
  }

  @ParameterizedTest
  @EnumSource(StorageType.class)
  void shouldReturnStorageNotFoundExceptionWhenGetStorageByType(StorageType type) {
    when(storageServiceClient.getByType(type)).thenReturn(Flux.empty());
    StepVerifier.create(storageManager.getByType(type))
        .expectError(EntityNotFoundException.class)
        .verify();
  }

  @ParameterizedTest
  @EnumSource(StorageType.class)
  void shouldGetById(StorageType type) {
    final long id = 123L;
    GetStorageDTO storage = storage(type);
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

  private final static Random RANDOM = new Random();

  private GetStorageDTO storage(StorageType type) {
    final long id = RANDOM.nextInt(1000);
    return new GetStorageDTO(id, "test-bucket-" + id, "files/", type);
  }

  private void assertStorage(GetStorageDTO expectValue, Mono<GetStorageDTO> actualValue) {
    StepVerifier.create(actualValue)
        .assertNext(result -> {
          assertEquals(expectValue.getId(), result.getId());
          assertEquals(expectValue.getType(), result.getType());
          assertEquals(expectValue.getBucket(), result.getBucket());
          assertEquals(expectValue.getPath(), result.getPath());
        }).verifyComplete();
  }
}
