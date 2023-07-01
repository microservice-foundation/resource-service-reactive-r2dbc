package com.epam.training.microservicefoundation.resourceservice.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.epam.training.microservicefoundation.resourceservice.common.CloudStorageExtension;
import com.epam.training.microservicefoundation.resourceservice.config.AwsS3Configuration;
import com.epam.training.microservicefoundation.resourceservice.config.properties.S3ClientConfigurationProperties;
import com.epam.training.microservicefoundation.resourceservice.model.ResourceFile;
import com.epam.training.microservicefoundation.resourceservice.model.StorageDTO;
import com.epam.training.microservicefoundation.resourceservice.model.StorageType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(value = {SpringExtension.class, CloudStorageExtension.class})
@EnableConfigurationProperties(S3ClientConfigurationProperties.class)
@ContextConfiguration(classes = {AwsS3Configuration.class})
@TestPropertySource(locations = "classpath:application.properties")
class CloudStorageRepositoryTest {
  @Autowired
  private CloudStorageRepository repository;
  @Value("${aws.s3.staging-bucket}")
  private String stagingBucket;
  @Value("${aws.s3.permanent-bucket}")
  private String permanentBucket;

  @Test
  void shouldUploadSong() throws IOException {
    final ResourceFile resourceFile = resourceFile();
    assertResourceFile(resourceFile, repository.upload(resourceFile));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void shouldUploadSongWithEmptyFilename(String filename) throws IOException {
    Path path = Paths.get("src/test/resources/files/mpthreetest.mp3");
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    FilePart filePart = new MockFilePart(filename, Files.readAllBytes(path), headers);
    ResourceFile resourceFile = new ResourceFile(filePart,
        StorageDTO.builder().bucket(stagingBucket).path("files/").type(StorageType.STAGING).build());

    assertResourceFile(resourceFile, repository.upload(resourceFile));
  }

  @Test
  void shouldGetSong() throws IOException {
    ResourceFile resourceFile = resourceFile();
    Mono<ResponsePublisher<GetObjectResponse>> responsePublisherMono = repository.upload(resourceFile)
        .flatMap(result -> repository.getByKey(result.getKey(), result.getStorage().getBucket()));

    StepVerifier.create(responsePublisherMono)
        .assertNext(result -> {
          assertNotNull(result.response());
          assertEquals(resourceFile.getFilePart().headers().getContentType().toString(), result.response().contentType());
          assertTrue(result.response().sdkHttpResponse().isSuccessful());
        })
        .verifyComplete();
  }

  @ParameterizedTest
  @EnumSource(StorageType.class)
  void shouldReturnNoSuchKeyExceptionWhenGetResourceFileByKey(StorageType type) {
    Mono<ResponsePublisher<GetObjectResponse>> responsePublisherMono = repository.getByKey(UUID.randomUUID().toString(),
        storage(type).getBucket());

    StepVerifier.create(responsePublisherMono)
        .expectError(NoSuchKeyException.class)
        .verify();
  }

  @Test
  void shouldReturnExceptionWhenGetResourceFileByKeyFromNonexistentBucket() throws IOException {
    ResourceFile resourceFile = resourceFile();
    Mono<ResponsePublisher<GetObjectResponse>> publisherMono =
        repository.upload(resourceFile).flatMap(result -> repository.getByKey(result.getKey(), "test"));

    StepVerifier.create(publisherMono)
        .expectError(S3Exception.class)
        .verify();
  }

  @Test
  void shouldDeleteSong() throws IOException {
    ResourceFile resourceFile = resourceFile();
    Mono<Void> voidMono = repository.upload(resourceFile)
        .flatMap(result -> repository.deleteByKey(result.getKey(), result.getStorage().getBucket()));

    StepVerifier.create(voidMono)
        .expectSubscription()
        .expectNextCount(0)
        .verifyComplete();
  }

  @ParameterizedTest
  @EnumSource(StorageType.class)
  void shouldDeleteSongWithNonexistentKey(StorageType type) {
    Mono<Void> voidMono = repository.deleteByKey(UUID.randomUUID().toString(), storage(type).getBucket());
    StepVerifier.create(voidMono)
        .expectSubscription()
        .expectNextCount(0)
        .verifyComplete();
  }

  @Test
  void shouldDeleteSongWithNonexistentBucket() throws IOException {
    ResourceFile resourceFile = resourceFile();
    Mono<Void> voidMono = repository.upload(resourceFile).flatMap(result -> repository.deleteByKey(result.getKey(), "test"));
    StepVerifier.create(voidMono)
        .expectError(NoSuchBucketException.class)
        .verify();
  }

  @Test
  void shouldMoveResourceFile() throws IOException {
    ResourceFile resourceFile = resourceFile();
    Mono<String> stringMono = repository.upload(resourceFile)
        .flatMap(result -> repository.move(result.getKey(), result.getStorage(), storage(StorageType.PERMANENT)));

    StepVerifier.create(stringMono)
        .assertNext(result -> {
          assertNotNull(result);
          assertThat(result, Matchers.startsWith(storage(StorageType.PERMANENT).getPath()));
        }).verifyComplete();
  }

  @Test
  void shouldMoveNonexistentResourceFileToPermanentStorage() {
    StorageDTO sourceStorage = storage(StorageType.STAGING);
    StorageDTO destinationStorage = storage(StorageType.PERMANENT);
    Mono<String> stringMono = repository.move(sourceStorage.getPath() + UUID.randomUUID(), sourceStorage, destinationStorage);

    StepVerifier.create(stringMono)
        .expectError(NoSuchKeyException.class)
        .verify();
  }

  @Test
  void shouldMoveResourceFileToNonexistentStorage() throws IOException {
    Mono<String> stringMono = repository.upload(resourceFile()).flatMap(result -> repository.move(result.getKey(), result.getStorage(),
        StorageDTO.builder().bucket("test").path("test/").build()));

    StepVerifier.create(stringMono)
        .expectError(NoSuchBucketException.class)
        .verify();
  }

  private StorageDTO storage(StorageType type) {
    return StorageDTO.builder()
        .type(type)
        .bucket(type == StorageType.PERMANENT ? permanentBucket : stagingBucket)
        .path(type == StorageType.PERMANENT ? "permanent-files/" : "staging-files/")
        .build();
  }

  private ResourceFile resourceFile() throws IOException {
    Path path = Paths.get("src/test/resources/files/mpthreetest.mp3");
    String fileName = "mpthreetest.mp3";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    FilePart filePart = new MockFilePart(fileName, Files.readAllBytes(path), headers);
    return new ResourceFile(filePart, storage(StorageType.STAGING));
  }

  private void assertResourceFile(ResourceFile expectedResult, Mono<ResourceFile> actualResult) {
    StepVerifier.create(actualResult)
        .assertNext(result -> {
          assertNotNull(result.getFilename());
          assertThat(result.getKey(), Matchers.startsWith(expectedResult.getStorage().getPath()));
        })
        .verifyComplete();
  }
}
