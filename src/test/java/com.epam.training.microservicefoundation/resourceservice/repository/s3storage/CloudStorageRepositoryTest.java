package com.epam.training.microservicefoundation.resourceservice.repository.s3storage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.epam.training.microservicefoundation.resourceservice.configuration.AwsS3Configuration;
import com.epam.training.microservicefoundation.resourceservice.configuration.S3ClientConfigurationProperties;
import com.epam.training.microservicefoundation.resourceservice.repository.CloudStorageRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
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
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@ExtendWith(value = {SpringExtension.class, CloudStorageExtension.class})
@EnableConfigurationProperties(S3ClientConfigurationProperties.class)
@ContextConfiguration(classes = {AwsS3Configuration.class})
@TestPropertySource(locations = "classpath:application.properties")
class CloudStorageRepositoryTest {
  @Autowired
  private CloudStorageRepository repository;

  @Test
  void shouldUploadSong() throws IOException {
    Path path = Paths.get("src/test/resources/files/mpthreetest.mp3");
    String fileName = "mpthreetest.mp3";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    FilePart filePart = new MockFilePart(fileName, Files.readAllBytes(path), headers);
    Mono<String> stringMono = repository.upload(filePart);

    StepVerifier.create(stringMono)
        .assertNext(result -> {
          assertThat(result, Matchers.notNullValue());
        })
        .verifyComplete();
  }

  @Test
  void shouldGetSong() throws IOException {
    // upload a file
    Path path = Paths.get("src/test/resources/files/mpthreetest.mp3");
    String fileName = "mpthreetest.mp3";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    FilePart filePart = new MockFilePart(fileName, Files.readAllBytes(path), headers);
    Mono<ResponsePublisher<GetObjectResponse>> responsePublisherMono = repository.upload(filePart).flatMap(repository::getByFileKey);

    StepVerifier.create(responsePublisherMono)
        .assertNext(result -> {
          assertNotNull(result.response());
          assertEquals(headers.getContentType().toString(), result.response().contentType());
          assertTrue(result.response().sdkHttpResponse().isSuccessful());
        })
        .verifyComplete();

  }

  @Test
  void shouldThrowExceptionWhenGetSongWithNonexistentKey() {
    Mono<ResponsePublisher<GetObjectResponse>> responsePublisherMono = repository.getByFileKey("nonexistent");
    StepVerifier.create(responsePublisherMono)
        .expectError(NoSuchKeyException.class)
        .verify();
  }

  @Test
  void shouldDeleteSong() throws IOException {
    // upload a file
    Path path = Paths.get("src/test/resources/files/mpthreetest.mp3");
    String fileName = "mpthreetest.mp3";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    FilePart filePart = new MockFilePart(fileName, Files.readAllBytes(path), headers);
    Mono<Void> voidMono = repository.upload(filePart).flatMap(repository::deleteByFileKey);

    StepVerifier.create(voidMono)
        .expectSubscription()
        .expectNoEvent(Duration.ofSeconds(1))
        .expectComplete();
  }

  @Test
  void shouldThrowExceptionWhenDeleteSongWithNonexistentKey() {
    Mono<Void> voidMono = repository.deleteByFileKey("nonexistent");
    StepVerifier.create(voidMono)
        .expectSubscription()
        .expectNoEvent(Duration.ofMillis(500))
        .expectComplete();
  }
}
