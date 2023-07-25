package com.epam.training.microservicefoundation.resourceservice.client;

import com.epam.training.microservicefoundation.resourceservice.model.dto.GetStorageDTO;
import com.epam.training.microservicefoundation.resourceservice.model.dto.StorageType;
import java.util.Collections;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import reactor.core.Exceptions;
import reactor.test.StepVerifier;

class StorageServiceClientCircuitBreakerTest extends BaseClientTest {
  @Autowired
  private StorageServiceClient client;

  @Test
  void shouldOpenStateOfCircuitBreakerWhenGetStorageByIdAfterRetries(@Server(service = Server.Service.STORAGE) MockServer storageServer) {
    storageServer.response(HttpStatus.SERVICE_UNAVAILABLE);
    storageServer.response(HttpStatus.INTERNAL_SERVER_ERROR);
    storageServer.response(HttpStatus.INTERNAL_SERVER_ERROR);
    storageServer.response(HttpStatus.INTERNAL_SERVER_ERROR);
    storageServer.response(HttpStatus.INTERNAL_SERVER_ERROR);
    storageServer.response(HttpStatus.OK, storage(StorageType.STAGING),
        Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    StepVerifier.create(client.getById(1234L))
        .consumeErrorWith(Exceptions::isRetryExhausted)
        .verify();
  }

  @Test
  void shouldHalfOpenToClosedStateOfCircuitBreakerWhenGetStorageByIdAfterRetries(
      @Server(service = Server.Service.STORAGE) MockServer storageServer) {
    storageServer.response(HttpStatus.SERVICE_UNAVAILABLE);
    storageServer.response(HttpStatus.INTERNAL_SERVER_ERROR);
    storageServer.response(HttpStatus.INTERNAL_SERVER_ERROR);
    storageServer.response(HttpStatus.INTERNAL_SERVER_ERROR);
    storageServer.response(HttpStatus.INTERNAL_SERVER_ERROR);

    StepVerifier.create(client.getById(1234L))
        .consumeErrorWith(Exceptions::isRetryExhausted)
        .verify();

    GetStorageDTO storage1 = storage(StorageType.STAGING);
    storageServer.response(HttpStatus.OK, storage1, Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
    StepVerifier.create(client.getById(123L))
        .expectNext(storage1)
        .verifyComplete();

    GetStorageDTO storage2 = storage(StorageType.PERMANENT);
    storageServer.response(HttpStatus.OK, storage2, Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
    StepVerifier.create(client.getById(124L))
        .expectNext(storage2)
        .verifyComplete();
  }

  private GetStorageDTO storage(StorageType type) {
    int id = new Random().nextInt(10000);
    return new GetStorageDTO(id, type.name().toLowerCase() + "-bucket" + id, "files/", type);
  }
}
