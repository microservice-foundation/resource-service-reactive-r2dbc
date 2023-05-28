package com.epam.training.microservicefoundation.resourceservice.api;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.epam.training.microservicefoundation.resourceservice.config.WebFluxConfiguration;
import com.epam.training.microservicefoundation.resourceservice.model.Mapper;
import com.epam.training.microservicefoundation.resourceservice.model.Resource;
import com.epam.training.microservicefoundation.resourceservice.model.ResourceRecord;
import com.epam.training.microservicefoundation.resourceservice.model.exception.DeleteFailedException;
import com.epam.training.microservicefoundation.resourceservice.model.exception.DownloadFailedException;
import com.epam.training.microservicefoundation.resourceservice.model.exception.UploadFailedException;
import com.epam.training.microservicefoundation.resourceservice.repository.CloudStorageRepository;
import com.epam.training.microservicefoundation.resourceservice.repository.ResourceRepository;
import com.epam.training.microservicefoundation.resourceservice.service.implementation.KafkaManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ResourceUtils;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.internal.async.SdkPublishers;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

@WebFluxTest
@DirtiesContext
@ContextConfiguration(classes = {WebFluxConfiguration.class})
@TestPropertySource(locations = "classpath:application.properties")
class ResourceApiTest {
  @MockBean
  private ResourceRepository resourceRepository;
  @MockBean
  private CloudStorageRepository cloudStorageRepository;
  @MockBean
  private KafkaManager kafkaManager;
  @MockBean
  private Mapper<Resource, ResourceRecord> mapper;

  @Autowired
  private WebTestClient webTestClient;

  @Test
  void shouldSaveResource() throws FileNotFoundException {
    String key = UUID.randomUUID().toString();
    long id = 1L;
    when(cloudStorageRepository.upload(any(FilePart.class))).thenReturn(Mono.just(key));
    when(resourceRepository.save(any(Resource.class))).thenReturn(Mono.just(new Resource.Builder(UUID.randomUUID().toString(),
        "mpthreetest.mp3").id(id).build()));

    when(mapper.mapToRecord(any(Resource.class))).thenReturn(new ResourceRecord(id));
    when(kafkaManager.publish(any())).thenReturn(Mono.empty());

    File file = ResourceUtils.getFile("classpath:files/mpthreetest.mp3");
    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("file", new FileSystemResource(file));

    webTestClient.post().uri("/api/v1/resources/")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(parts))
        .exchange()
        .expectStatus().isCreated()
        .expectBody().jsonPath("$.id").isEqualTo(id);
  }

  @Test
  void shouldThrowValidationExceptionWhenSaveResource() throws FileNotFoundException {
    File file = ResourceUtils.getFile("classpath:files/mpthreetest.mp3");
    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("file", file);

    webTestClient.post().uri("/api/v1/resources/")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(parts))
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.status").isEqualTo("BAD_REQUEST")
        .jsonPath("$.message").isEqualTo("Invalid request")
        .jsonPath("$.debugMessage").isEqualTo("File is not validated, please check your file");
  }

  @Test
  void shouldThrowValidationExceptionWhenSaveResourceWithEmptyFile() throws FileNotFoundException {
    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("file", new byte[0]);

    webTestClient.post().uri("/api/v1/resources/")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(parts))
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.status").isEqualTo("BAD_REQUEST")
        .jsonPath("$.message").isEqualTo("Invalid request")
        .jsonPath("$.debugMessage").isEqualTo("File is not validated, please check your file");
  }

  @Test
  void shouldThrowValidationExceptionWhenSaveInvalidTypeResource() {
    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("file", "this is string");

    webTestClient.post().uri("/api/v1/resources/")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(parts))
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.status").isEqualTo("BAD_REQUEST")
        .jsonPath("$.message").isEqualTo("Invalid request")
        .jsonPath("$.debugMessage").isEqualTo("File is not validated, please check your file");
  }

  @Test
  void shouldThrowExceptionWhenSaveTwoResourcesWithSameName() throws Exception {
    when(cloudStorageRepository.upload(any(FilePart.class))).thenReturn(Mono.just(UUID.randomUUID().toString()));
    when(resourceRepository.save(any(Resource.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint \"resources_name_key\""));

    File file2 = ResourceUtils.getFile("classpath:files/mpthreetest.mp3");
    MultiValueMap<String, Object> parts2 = new LinkedMultiValueMap<>();
    parts2.add("file", new FileSystemResource(file2));

    webTestClient.post().uri("/api/v1/resources/")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(parts2))
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.status").isEqualTo("BAD_REQUEST")
        .jsonPath("$.message").isEqualTo("Invalid request")
        .jsonPath("$.debugMessage").value(
            containsString("duplicate key value violates unique constraint \"resources_name_key\""));
  }

  @Test
  void shouldThrowUploadFailedExceptionWhenSaveResource() throws FileNotFoundException {
    when(cloudStorageRepository.upload(any(FilePart.class))).thenThrow(new UploadFailedException(UploadPartResponse.builder().build()));
    File file2 = ResourceUtils.getFile("classpath:files/mpthreetest.mp3");
    MultiValueMap<String, Object> parts2 = new LinkedMultiValueMap<>();
    parts2.add("file", new FileSystemResource(file2));

    webTestClient.post().uri("/api/v1/resources/")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(parts2))
        .exchange()
        .expectStatus().is5xxServerError()
        .expectBody()
        .jsonPath("$.status").isEqualTo("INTERNAL_SERVER_ERROR")
        .jsonPath("$.message").value(containsString("Resource file upload has failed"));

  }

  @Test
  void shouldDeleteResourceByIds() {
    long[] ids = {1L, 2L};
    Resource resource1 = new Resource.Builder(UUID.randomUUID().toString(), "resource-1").id(ids[0]).build();
    when(resourceRepository.findById(ids[0])).thenReturn(Mono.just(resource1));
    Resource resource2 = new Resource.Builder(UUID.randomUUID().toString(), "resource-2").id(ids[1]).build();
    when(resourceRepository.findById(ids[1])).thenReturn(Mono.just(resource2));
    when(cloudStorageRepository.deleteByFileKey(resource1.getKey())).thenReturn(Mono.empty());
    when(cloudStorageRepository.deleteByFileKey(resource2.getKey())).thenReturn(Mono.empty());
    when(resourceRepository.delete(resource1)).thenReturn(Mono.empty());
    when(resourceRepository.delete(resource2)).thenReturn(Mono.empty());

    webTestClient.delete().uri(uriBuilder -> uriBuilder
            .path("/api/v1/resources")
            .queryParam("id", ids[0] + "," + ids[1])
            .build())
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$[*].id").value(containsInAnyOrder(
            is((int) ids[0]),
            is((int) ids[1])));
  }

  @Test
  void shouldThrowExceptionWhenDeleteResourceByEmptyIdsList() {
    webTestClient.delete().uri(uriBuilder -> uriBuilder.path("/api/v1/resources").queryParam("id", "").build())
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.status").isEqualTo("BAD_REQUEST")
        .jsonPath("$.message").isEqualTo("Invalid request")
        .jsonPath("$.debugMessage").isEqualTo("For input string: \"\"");
  }

  @Test
  void shouldReturnEmptyWhenDeleteResourceByNegativeIds() {
    when(resourceRepository.findById(anyLong())).thenReturn(Mono.empty());
    webTestClient.delete().uri(uriBuilder -> uriBuilder
            .path("/api/v1/resources")
            .queryParam("id", "-1,-3")
            .build())
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void shouldThrowDeleteFailedExceptionWhenDeleteResourceByIds() {
    Resource resource = new Resource.Builder(UUID.randomUUID().toString(), "resource").build();
    when(resourceRepository.findById(anyLong())).thenReturn(Mono.just(resource));
    when(cloudStorageRepository.deleteByFileKey(anyString())).thenThrow(new DeleteFailedException(DeleteObjectResponse.builder()
        .build()));
    webTestClient.delete().uri(uriBuilder -> uriBuilder
            .path("/api/v1/resources")
            .queryParam("id", "1,2")
            .build())
        .exchange()
        .expectStatus().is5xxServerError()
        .expectBody()
        .jsonPath("$.status").isEqualTo("INTERNAL_SERVER_ERROR")
        .jsonPath("$.message").value(containsString("Resource file deletion has failed"));
  }

  @Test
  void shouldGetResource() throws IOException {
    long id = 1L;
    Resource resource = new Resource.Builder(UUID.randomUUID().toString(), "resource").id(id).build();
    when(resourceRepository.findById(id)).thenReturn(Mono.just(resource));

    Path path = Paths.get("src/test/resources/files/mpthreetest.mp3");
    GetObjectResponse getObjectResponse = GetObjectResponse.builder()
        .contentType(MediaType.MULTIPART_FORM_DATA.toString())
        .contentLength(path.toFile().length())
        .build();

    SdkPublisher<ByteBuffer> byteBufferSdkPublisher =
        SdkPublishers.envelopeWrappedPublisher(Mono.just(ByteBuffer.wrap(Files.readAllBytes(path))), "", "");

    when(cloudStorageRepository.getByFileKey(resource.getKey())).thenReturn(Mono.just(new ResponsePublisher<>(getObjectResponse,
        byteBufferSdkPublisher)));

    webTestClient.get().uri("/api/v1/resources/{id}", id)
        .accept(MediaType.APPLICATION_OCTET_STREAM)
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader().contentLength(path.toFile().length());
  }

  @Test
  void shouldThrowExceptionWhenGetById() {
    long id = 124567L;
    when(resourceRepository.findById(id)).thenReturn(Mono.empty());
    webTestClient.get().uri("/api/v1/resources/{id}", id)
        .accept(MediaType.APPLICATION_OCTET_STREAM)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.status").isEqualTo("NOT_FOUND")
        .jsonPath("$.message").isEqualTo("Resource is not found")
        .jsonPath("$.debugMessage").isEqualTo("Resource is not found with id '124567'");
  }

  @Test
  void shouldThrowDownloadFailedExceptionWhenGetById() {
    long id = 1L;
    Resource resource = new Resource.Builder(UUID.randomUUID().toString(), "resource").id(id).build();
    when(resourceRepository.findById(id)).thenReturn(Mono.just(resource));
    when(cloudStorageRepository.getByFileKey(resource.getKey())).thenThrow(
        new DownloadFailedException(GetObjectResponse.builder().build()));
    webTestClient.get().uri("/api/v1/resources/{id}", id)
        .accept(MediaType.APPLICATION_OCTET_STREAM)
        .exchange()
        .expectStatus().is5xxServerError()
        .expectBody()
        .jsonPath("$.status").isEqualTo("INTERNAL_SERVER_ERROR")
        .jsonPath("$.message").value(containsString("Resource file download has failed"));
  }
}
