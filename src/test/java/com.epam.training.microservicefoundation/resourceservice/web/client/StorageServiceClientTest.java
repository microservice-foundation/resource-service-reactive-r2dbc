package com.epam.training.microservicefoundation.resourceservice.web.client;

import com.epam.training.microservicefoundation.resourceservice.web.client.Server.Service;
import com.epam.training.microservicefoundation.resourceservice.domain.dto.GetStorageDTO;
import com.epam.training.microservicefoundation.resourceservice.domain.dto.StorageType;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import reactor.core.Exceptions;
import reactor.test.StepVerifier;

class StorageServiceClientTest extends BaseClientTest {
  @Autowired
  private StorageServiceClient client;

  @Test
  void shouldGetStagingStorage(@Server(service = Service.STORAGE) MockServer storageServer) {
    GetStorageDTO storage1 = storage(StorageType.STAGING);
    GetStorageDTO storage2 = storage(StorageType.STAGING);
    List<GetStorageDTO> storages = List.of(storage1, storage2);
    storageServer.response(HttpStatus.OK, storages, Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    StepVerifier.create(client.getByType(StorageType.STAGING))
        .expectNextSequence(storages)
        .verifyComplete();
  }

  @Test
  void shouldGetStagingStorageAfterRetries(@Server(service = Service.STORAGE) MockServer storageServer) {
    GetStorageDTO storage1 = storage(StorageType.STAGING);
    GetStorageDTO storage2 = storage(StorageType.STAGING);

    List<GetStorageDTO> storages = List.of(storage1, storage2);
    storageServer.response(HttpStatus.SERVICE_UNAVAILABLE);
    storageServer.response(HttpStatus.NOT_FOUND);
    storageServer.response(HttpStatus.OK, storages, Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    StepVerifier.create(client.getByType(StorageType.STAGING))
        .expectNextSequence(storages)
        .verifyComplete();
  }

  @Test
  void shouldGetPermanentStorage(@Server(service = Service.STORAGE) MockServer storageServer) {
    GetStorageDTO storage1 = storage(StorageType.PERMANENT);
    GetStorageDTO storage2 = storage(StorageType.PERMANENT);
    List<GetStorageDTO> storages = List.of(storage1, storage2);
    storageServer.response(HttpStatus.OK, storages, Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    StepVerifier.create(client.getByType(StorageType.PERMANENT))
        .expectNextSequence(storages)
        .verifyComplete();
  }

  @Test
  void shouldGetPermanentStorageAfterRetries(@Server(service = Service.STORAGE) MockServer storageServer) {
    storageServer.response(HttpStatus.NOT_FOUND);
    storageServer.response(HttpStatus.SERVICE_UNAVAILABLE);
    GetStorageDTO storage1 = storage(StorageType.PERMANENT);
    GetStorageDTO storage2 = storage(StorageType.PERMANENT);
    List<GetStorageDTO> storages = List.of(storage1, storage2);
    storageServer.response(HttpStatus.OK, storages, Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    StepVerifier.create(client.getByType(StorageType.PERMANENT))
        .expectNextSequence(storages)
        .verifyComplete();
  }

  @Test
  void shouldGetEmptyStorageAfterRetries(@Server(service = Service.STORAGE) MockServer storageServer) {
    storageServer.response(HttpStatus.SERVICE_UNAVAILABLE);
    storageServer.response(HttpStatus.NOT_FOUND);

    StepVerifier.create(client.getByType(StorageType.STAGING))
        .consumeErrorWith(Exceptions::isRetryExhausted)
        .verify();
  }

  @Test
  void shouldGetById(@Server(service = Service.STORAGE) MockServer storageServer) {
    GetStorageDTO storage1 = storage(StorageType.PERMANENT);
    storageServer.response(HttpStatus.OK, storage1, Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
    StepVerifier.create(client.getById(storage1.getId()))
        .expectNext(storage1)
        .verifyComplete();

    GetStorageDTO storage2 = storage(StorageType.STAGING);
    storageServer.response(HttpStatus.OK, storage2, Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
    StepVerifier.create(client.getById(storage2.getId()))
        .expectNext(storage2)
        .verifyComplete();
  }

  @Test
  void shouldGetByIdAfterRetries(@Server(service = Service.STORAGE) MockServer storageServer) {
    storageServer.response(HttpStatus.INTERNAL_SERVER_ERROR);
    storageServer.response(HttpStatus.NOT_FOUND);
    GetStorageDTO storage1 = storage(StorageType.PERMANENT);
    storageServer.response(HttpStatus.OK, storage1, Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
    StepVerifier.create(client.getById(storage1.getId()))
        .expectNext(storage1)
        .verifyComplete();

    storageServer.response(HttpStatus.SERVICE_UNAVAILABLE);
    storageServer.response(HttpStatus.NOT_FOUND);
    GetStorageDTO storage2 = storage(StorageType.STAGING);
    storageServer.response(HttpStatus.OK, storage2, Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
    StepVerifier.create(client.getById(storage2.getId()))
        .expectNext(storage2)
        .verifyComplete();
  }

  @Test
  void shouldGetEmptyStorageByIdAfterRetries(@Server(service = Service.STORAGE) MockServer storageServer) {
    storageServer.response(HttpStatus.SERVICE_UNAVAILABLE);
    storageServer.response(HttpStatus.NOT_FOUND);

    StepVerifier.create(client.getById(1234L))
        .consumeErrorWith(Exceptions::isRetryExhausted)
        .verify();
  }

  private GetStorageDTO storage(StorageType type) {
    int id = new Random().nextInt(10000);
    return new GetStorageDTO(id, type.name().toLowerCase() + "-bucket" + id, "files/", type);
  }
}
