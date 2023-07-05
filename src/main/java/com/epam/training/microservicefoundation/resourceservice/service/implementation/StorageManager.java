package com.epam.training.microservicefoundation.resourceservice.service.implementation;

import com.epam.training.microservicefoundation.resourceservice.client.StorageServiceClient;
import com.epam.training.microservicefoundation.resourceservice.model.StorageDTO;
import com.epam.training.microservicefoundation.resourceservice.model.StorageType;
import com.epam.training.microservicefoundation.resourceservice.model.exception.StorageNotFoundException;
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
  private final Map<StorageType, List<StorageDTO>> storageCache = new HashMap<>();
  private static final Random RANDOM = new Random();

  @Autowired
  public StorageManager(StorageServiceClient storageServiceClient) {
    this.storageServiceClient = storageServiceClient;
  }

  public Mono<StorageDTO> getByType(StorageType type) {
    if (!storageCache.containsKey(type)) {
      return storageServiceClient.getByType(type)
          .collectList()
          .doOnNext(storages -> storageCache.put(type, storages))
          .map(storages -> storageCache.get(type).get(randomElementIndex(storageCache.get(type).size())))
          .onErrorMap(IndexOutOfBoundsException.class,
              error -> new StorageNotFoundException(String.format("Storage is not found by '%s' type", type), error))
          .onErrorMap(Exceptions::isRetryExhausted,
              error -> new IllegalStateException(String.format("Retry to gat storage by type '%s' is exhausted", type), error));
    }
    return Mono.just(storageCache.get(type).get(randomElementIndex(storageCache.get(type).size())));
  }

  private int randomElementIndex(int size) {
    return RANDOM.nextInt(size);
  }

  public Mono<StorageDTO> getById(long id) {
    return storageServiceClient.getById(id)
        .onErrorMap(Exceptions::isRetryExhausted,
            error -> new IllegalStateException(String.format("Retry to gat storage by id '%d' is exhausted", id), error));
  }
}
