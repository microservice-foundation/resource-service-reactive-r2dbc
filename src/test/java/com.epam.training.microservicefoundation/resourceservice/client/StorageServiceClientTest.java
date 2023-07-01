package com.epam.training.microservicefoundation.resourceservice.client;

import com.epam.training.microservicefoundation.resourceservice.client.Server.Service;
import com.epam.training.microservicefoundation.resourceservice.config.ClientConfiguration;
import com.epam.training.microservicefoundation.resourceservice.config.properties.WebClientProperties;
import com.epam.training.microservicefoundation.resourceservice.model.StorageDTO;
import com.epam.training.microservicefoundation.resourceservice.model.StorageType;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.config.client.RetryProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.Exceptions;
import reactor.test.StepVerifier;

@ExtendWith({MockServerExtension.class, SpringExtension.class})
@EnableConfigurationProperties({WebClientProperties.class, RetryProperties.class})
@ContextConfiguration(classes = ClientConfiguration.class)
@TestPropertySource(locations = "classpath:application.properties")
class StorageServiceClientTest {

  @Autowired
  private StorageServiceClient client;

  @Test
  void shouldGetStagingStorage(@Server(service = Service.STORAGE) MockServer storageServer) {
    StorageDTO storage1 = storage(StorageType.STAGING);
    StorageDTO storage2 = storage(StorageType.STAGING);
    List<StorageDTO> storages = List.of(storage1, storage2);
    storageServer.response(HttpStatus.OK, storages, Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    StepVerifier.create(client.getByType(StorageType.STAGING))
        .expectNextSequence(storages)
        .verifyComplete();
  }

  @Test
  void shouldGetStagingStorageAfterRetries(@Server(service = Service.STORAGE) MockServer storageServer) {
    StorageDTO storage1 = storage(StorageType.STAGING);
    StorageDTO storage2 = storage(StorageType.STAGING);

    List<StorageDTO> storages = List.of(storage1, storage2);
    storageServer.response(HttpStatus.SERVICE_UNAVAILABLE);
    storageServer.response(HttpStatus.NOT_FOUND);
    storageServer.response(HttpStatus.OK, storages, Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    StepVerifier.create(client.getByType(StorageType.STAGING))
        .expectNextSequence(storages)
        .verifyComplete();
  }

  @Test
  void shouldGetPermanentStorage(@Server(service = Service.STORAGE) MockServer storageServer) {
    StorageDTO storage1 = storage(StorageType.PERMANENT);
    StorageDTO storage2 = storage(StorageType.PERMANENT);
    List<StorageDTO> storages = List.of(storage1, storage2);
    storageServer.response(HttpStatus.OK, storages, Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    StepVerifier.create(client.getByType(StorageType.PERMANENT))
        .expectNextSequence(storages)
        .verifyComplete();
  }

  @Test
  void shouldGetPermanentStorageAfterRetries(@Server(service = Service.STORAGE) MockServer storageServer) {
    StorageDTO storage1 = storage(StorageType.PERMANENT);
    StorageDTO storage2 = storage(StorageType.PERMANENT);

    storageServer.response(HttpStatus.NOT_FOUND);
    storageServer.response(HttpStatus.SERVICE_UNAVAILABLE);
    List<StorageDTO> storages = List.of(storage1, storage2);
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
    StorageDTO storage1 = storage(StorageType.PERMANENT);
    storageServer.response(HttpStatus.OK, storage1, Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
    StepVerifier.create(client.getById(storage1.getId()))
        .expectNext(storage1)
        .verifyComplete();

    StorageDTO storage2 = storage(StorageType.STAGING);
    storageServer.response(HttpStatus.OK, storage2, Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
    StepVerifier.create(client.getById(storage2.getId()))
        .expectNext(storage2)
        .verifyComplete();
  }

  @Test
  void shouldGetByIdAfterRetries(@Server(service = Service.STORAGE) MockServer storageServer) {
    storageServer.response(HttpStatus.INTERNAL_SERVER_ERROR);
    storageServer.response(HttpStatus.NOT_FOUND);
    StorageDTO storage1 = storage(StorageType.PERMANENT);
    storageServer.response(HttpStatus.OK, storage1, Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
    StepVerifier.create(client.getById(storage1.getId()))
        .expectNext(storage1)
        .verifyComplete();

    storageServer.response(HttpStatus.SERVICE_UNAVAILABLE);
    storageServer.response(HttpStatus.NOT_FOUND);
    StorageDTO storage2 = storage(StorageType.STAGING);
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

  private StorageDTO storage(StorageType type) {
    int id = new Random().nextInt(10000);
    return StorageDTO.builder().id(id).path("files/").bucket(type.name().toLowerCase() + "-bucket" + id)
        .type(type).build();
  }
}
