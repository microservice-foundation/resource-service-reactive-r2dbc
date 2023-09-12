package com.epam.training.microservicefoundation.resourceservice.service.implementation;

import com.epam.training.microservicefoundation.resourceservice.web.client.StorageServiceClient;
import com.epam.training.microservicefoundation.resourceservice.domain.dto.GetStorageDTO;
import com.epam.training.microservicefoundation.resourceservice.domain.dto.StorageType;
import com.epam.training.microservicefoundation.resourceservice.domain.exception.ExceptionSupplier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@Service
public class StorageManager {
  private final StorageServiceClient storageServiceClient;
  private final Map<StorageType, List<GetStorageDTO>> storageCache = new HashMap<>();
  private static final Random RANDOM = new Random();

  @Autowired
  public StorageManager(StorageServiceClient storageServiceClient) {
    this.storageServiceClient = storageServiceClient;
  }

  public Mono<GetStorageDTO> getByType(StorageType type) {
    if (!storageCache.containsKey(type)) {
      return storageServiceClient.getByType(type)
          .collectList()
          .doOnNext(storages -> storageCache.put(type, storages))
          .map(storages -> storageCache.get(type).get(randomElementIndex(storageCache.get(type).size())))
          .onErrorMap(IndexOutOfBoundsException.class, error -> ExceptionSupplier.entityNotFound(GetStorageDTO.class, type).get())
          .onErrorMap(Exceptions::isRetryExhausted, error -> ExceptionSupplier.retryExhausted(error).get());
    }
    return Mono.just(storageCache.get(type).get(randomElementIndex(storageCache.get(type).size())));
  }

  private int randomElementIndex(int size) {
    return size > 0 ? RANDOM.nextInt(size) : size;
  }

  public Mono<GetStorageDTO> getById(long id) {
    return storageServiceClient.getById(id)
        .onErrorMap(Exceptions::isRetryExhausted, error -> ExceptionSupplier.retryExhausted(error).get());
  }
}
