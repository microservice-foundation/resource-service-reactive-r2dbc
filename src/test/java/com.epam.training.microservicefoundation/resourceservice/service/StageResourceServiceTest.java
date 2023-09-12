package com.epam.training.microservicefoundation.resourceservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.epam.training.microservicefoundation.resourceservice.common.FakeSenderResult;
import com.epam.training.microservicefoundation.resourceservice.kafka.producer.KafkaProducer;
import com.epam.training.microservicefoundation.resourceservice.service.mapper.GetResourceMapper;
import com.epam.training.microservicefoundation.resourceservice.domain.ResourceFile;
import com.epam.training.microservicefoundation.resourceservice.domain.dto.GetResourceDTO;
import com.epam.training.microservicefoundation.resourceservice.domain.dto.GetStorageDTO;
import com.epam.training.microservicefoundation.resourceservice.domain.dto.StorageType;
import com.epam.training.microservicefoundation.resourceservice.domain.entity.Resource;
import com.epam.training.microservicefoundation.resourceservice.domain.entity.ResourceStatus;
import com.epam.training.microservicefoundation.resourceservice.domain.exception.CloudStorageException;
import com.epam.training.microservicefoundation.resourceservice.repository.CloudStorageRepository;
import com.epam.training.microservicefoundation.resourceservice.common.FakeFilePart;
import com.epam.training.microservicefoundation.resourceservice.repository.ResourceRepository;
import com.epam.training.microservicefoundation.resourceservice.service.implementation.StageResourceService;
import com.epam.training.microservicefoundation.resourceservice.service.implementation.StorageManager;
import java.io.IOException;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class StageResourceServiceTest {

  @Mock
  private ResourceRepository resourceRepository;
  @Mock
  private CloudStorageRepository cloudStorageRepository;
  @Mock
  private KafkaProducer kafkaProducer;
  @Mock
  private StorageManager storageManager;
  @Mock
  private GetResourceMapper getResourceMapper;
  @InjectMocks
  private StageResourceService stageResourceService;

  private static final String FILENAME = "mpthreetest.mp3";
  private static final Path FILE_PATH = Paths.get("src/test/resources/files/mpthreetest.mp3");
  private static final GetStorageDTO STAGING_STORAGE = new GetStorageDTO(998L, "resource-staging", "files/", StorageType.STAGING);


  @Test
  void shouldSaveSong() throws IOException {
    final long id = 1L;
    final String fileKey = UUID.randomUUID().toString();
    final FilePart filePart = filePart();

    when(resourceRepository.existsByName(filePart.filename())).thenReturn(Mono.just(false));
    when(storageManager.getByType(StorageType.STAGING)).thenReturn(Mono.just(STAGING_STORAGE));
    when(cloudStorageRepository.upload(any(ResourceFile.class))).thenReturn(Mono.just(new ResourceFile(filePart, STAGING_STORAGE)
        .withKey(fileKey)));
    final Resource savedResource = getSavedResource();
    when(resourceRepository.save(any())).thenReturn(Mono.just(savedResource));
    when(getResourceMapper.toDto(any())).thenReturn(new GetResourceDTO(id));
    when(kafkaProducer.publish(any())).thenReturn(Mono.just(new FakeSenderResult<>(null, null, null)));

    StepVerifier.create(stageResourceService.saveToStage(Mono.just(filePart)))
        .assertNext(result -> assertEquals(id, result.getId()))
        .verifyComplete();
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenSaveEmptyMono() {
    StepVerifier.create(stageResourceService.saveToStage(Mono.empty()))
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenSaveExistentResource() throws IOException {
    final FilePart filePart = filePart();
    when(resourceRepository.existsByName(filePart.filename())).thenReturn(Mono.just(true));

    StepVerifier.create(stageResourceService.saveToStage(Mono.just(filePart)))
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenSaveAndGetStorage() throws IOException {
    final FilePart filePart = filePart();
    when(resourceRepository.existsByName(filePart.filename())).thenReturn(Mono.just(false));
    when(storageManager.getByType(StorageType.STAGING)).thenReturn(Mono.empty());

    StepVerifier.create(stageResourceService.saveToStage(Mono.just(filePart)))
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void shouldThrowUploadFailedExceptionWhenSaveSong() throws IOException {
    final FilePart filePart = filePart();
    when(resourceRepository.existsByName(filePart.filename())).thenReturn(Mono.just(false));
    when(storageManager.getByType(StorageType.STAGING)).thenReturn(Mono.just(STAGING_STORAGE));
    when(cloudStorageRepository.upload(any(ResourceFile.class))).thenThrow(CloudStorageException.class);

    StepVerifier.create(stageResourceService.saveToStage(Mono.just(filePart)))
        .expectError(CloudStorageException.class)
        .verify();
  }

  @Test
  void shouldThrowDataIntegrityViolationExceptionWhenSaveSong() throws IOException {
    final String fileKey = UUID.randomUUID().toString();
    final FilePart filePart = filePart();

    when(resourceRepository.existsByName(filePart.filename())).thenReturn(Mono.just(false));
    when(storageManager.getByType(StorageType.STAGING)).thenReturn(Mono.just(STAGING_STORAGE));
    when(cloudStorageRepository.upload(any(ResourceFile.class))).thenReturn(Mono.just(new ResourceFile(filePart, STAGING_STORAGE)
        .withKey(fileKey)));
    when(resourceRepository.save(any())).thenThrow(DataIntegrityViolationException.class);

    StepVerifier.create(stageResourceService.saveToStage(Mono.just(filePart)))
        .expectError(DataIntegrityViolationException.class)
        .verify();
  }

  @Test
  void shouldThrowIllegalStateExceptionWhenSaveSong() throws IOException {
    final String fileKey = UUID.randomUUID().toString();
    final FilePart filePart = filePart();

    when(resourceRepository.existsByName(filePart.filename())).thenReturn(Mono.just(false));
    when(storageManager.getByType(StorageType.STAGING)).thenReturn(Mono.just(STAGING_STORAGE));
    when(cloudStorageRepository.upload(any(ResourceFile.class))).thenReturn(Mono.just(new ResourceFile(filePart, STAGING_STORAGE)
        .withKey(fileKey)));
    final Resource savedResource = getSavedResource();
    when(resourceRepository.save(any())).thenReturn(Mono.just(savedResource));
    when(kafkaProducer.publish(any())).thenThrow(IllegalStateException.class);

    StepVerifier.create(stageResourceService.saveToStage(Mono.just(filePart)))
        .expectError(IllegalStateException.class)
        .verify();
  }

  private static final Random RANDOM = new Random();
  private Resource getSavedResource() {
    int id = RANDOM.nextInt(1000);
    return Resource.builder().status(ResourceStatus.PROCESSED).key(UUID.randomUUID().toString()).id(id)
        .storageId(RANDOM.nextInt(1000)).name("test" + id).build();
  }

  private FilePart filePart() throws IOException {
    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    return new FakeFilePart(FILENAME, Files.readAllBytes(FILE_PATH), headers);
  }
}
