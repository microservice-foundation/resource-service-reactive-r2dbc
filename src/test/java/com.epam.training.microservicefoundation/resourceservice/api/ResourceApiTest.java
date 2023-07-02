package com.epam.training.microservicefoundation.resourceservice.api;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.epam.training.microservicefoundation.resourceservice.config.WebFluxConfiguration;
import com.epam.training.microservicefoundation.resourceservice.model.ResourceDTO;
import com.epam.training.microservicefoundation.resourceservice.model.exception.DeleteFailedException;
import com.epam.training.microservicefoundation.resourceservice.model.exception.DownloadFailedException;
import com.epam.training.microservicefoundation.resourceservice.model.exception.ResourceNotFoundException;
import com.epam.training.microservicefoundation.resourceservice.model.exception.StorageNotFoundException;
import com.epam.training.microservicefoundation.resourceservice.model.exception.UploadFailedException;
import com.epam.training.microservicefoundation.resourceservice.service.ResourceService;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ResourceUtils;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.internal.async.SdkPublishers;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

@WebFluxTest
@DirtiesContext
@ContextConfiguration(classes = {WebFluxConfiguration.class})
@TestPropertySource(locations = "classpath:application.properties")
class ResourceApiTest {
  @MockBean
  private ResourceService resourceService;
  @Autowired
  private WebTestClient webTestClient;

  @Test
  void shouldSaveResource() throws FileNotFoundException {
    final long id = 1L;
    when(resourceService.save(any())).thenReturn(Mono.just(new ResourceDTO(id)));
    webTestClient.post().uri("/api/v1/resources/")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multiParts()))
        .exchange()
        .expectStatus().isCreated()
        .expectBody().jsonPath("$.id").isEqualTo(id);
  }

  @Test
  void shouldThrowValidationExceptionWhenSaveResource() throws FileNotFoundException {
    when(resourceService.save(any())).thenThrow(new IllegalArgumentException("File is not validated, please check your file"));

    webTestClient.post().uri("/api/v1/resources/")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multiParts()))
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.status").isEqualTo("BAD_REQUEST")
        .jsonPath("$.message").isEqualTo("Invalid request")
        .jsonPath("$.debugMessage").isEqualTo("File is not validated, please check your file");
  }

  @Test
  void shouldThrowStorageNotFoundExceptionWhenSaveResource() throws FileNotFoundException {
    when(resourceService.save(any())).thenThrow(new StorageNotFoundException("Storage is not found by 'STAGING' type"));
    webTestClient.post().uri("/api/v1/resources/")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multiParts()))
        .exchange()
        .expectStatus().isNotFound()
        .expectBody()
        .jsonPath("$.status").isEqualTo("NOT_FOUND")
        .jsonPath("$.message").isEqualTo("Storage is not found")
        .jsonPath("$.debugMessage").isEqualTo("Storage is not found by 'STAGING' type");
  }

  @Test
  void shouldThrowIllegalStateExceptionWhenGetStorageToSaveResource() throws FileNotFoundException {
    when(resourceService.save(any())).thenThrow(new IllegalStateException("Retry to gat storage by type 'STAGING' is exhausted"));
    webTestClient.post().uri("/api/v1/resources/")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multiParts()))
        .exchange()
        .expectStatus().is5xxServerError()
        .expectBody()
        .jsonPath("$.status").isEqualTo("INTERNAL_SERVER_ERROR")
        .jsonPath("$.message").isEqualTo("Internal server error has happened")
        .jsonPath("$.debugMessage").isEqualTo("Retry to gat storage by type 'STAGING' is exhausted");
  }

  @Test
  void shouldThrowUploadFailedExceptionWhenSaveResource() throws FileNotFoundException {
    SdkResponse sdkResponse = UploadPartResponse.builder().sdkHttpResponse(SdkHttpResponse.builder()
        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value()).statusText("Resource file upload has failed").build()).build();

    when(resourceService.save(any())).thenThrow(new UploadFailedException(sdkResponse));

    webTestClient.post().uri("/api/v1/resources/")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multiParts()))
        .exchange()
        .expectStatus().is5xxServerError()
        .expectBody()
        .jsonPath("$.status").isEqualTo("INTERNAL_SERVER_ERROR")
        .jsonPath("$.message").value(containsString("Resource file upload has failed"));
  }

  @Test
  void shouldDeleteResourceByIds() {
    long[] ids = {1L, 2L};
    when(resourceService.deleteByIds(any(Flux.class))).thenReturn(Flux.just(new ResourceDTO(ids[0]), new ResourceDTO(ids[1])));

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
  void shouldThrowIllegalArgumentExceptionWhenDeleteResourceByIds() {
    when(resourceService.deleteByIds(any(Flux.class))).thenThrow(new IllegalArgumentException("Id param is not validated, check your ids"));
    webTestClient.delete().uri(uriBuilder -> uriBuilder.path("/api/v1/resources").queryParam("id", "").build())
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.status").isEqualTo("BAD_REQUEST")
        .jsonPath("$.message").isEqualTo("Invalid request")
        .jsonPath("$.debugMessage").isEqualTo("Id param is not validated, check your ids");
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
        .jsonPath("$.debugMessage").isEqualTo("'producer' must not be null");
  }

  @Test
  void shouldThrowDeleteFailedExceptionWhenDeleteResourceByIds() {
    SdkResponse sdkResponse = UploadPartResponse.builder().sdkHttpResponse(SdkHttpResponse.builder()
        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value()).statusText("Resource file deletion has failed").build()).build();
    when(resourceService.deleteByIds(any(Flux.class))).thenThrow(new DeleteFailedException(sdkResponse));
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
  void shouldThrowIllegalStateExceptionWhenDeleteResourceByIds() {
    when(resourceService.deleteByIds(any(Flux.class))).thenThrow(new IllegalStateException("Retry to gat storage by id '123' is exhausted"));
    webTestClient.delete().uri(uriBuilder -> uriBuilder
            .path("/api/v1/resources")
            .queryParam("id", "1,2")
            .build())
        .exchange()
        .expectStatus().is5xxServerError()
        .expectBody()
        .jsonPath("$.status").isEqualTo("INTERNAL_SERVER_ERROR")
        .jsonPath("$.message").isEqualTo("Internal server error has happened")
        .jsonPath("$.debugMessage").isEqualTo("Retry to gat storage by id '123' is exhausted");
  }

  @Test
  void shouldGetResource() throws IOException {
    long id = 1L;
    Path path = Paths.get("src/test/resources/files/mpthreetest.mp3");
    GetObjectResponse getObjectResponse = GetObjectResponse.builder()
        .contentType(MediaType.MULTIPART_FORM_DATA.toString())
        .contentLength(path.toFile().length())
        .build();

    SdkPublisher<ByteBuffer> byteBufferSdkPublisher =
        SdkPublishers.envelopeWrappedPublisher(Mono.just(ByteBuffer.wrap(Files.readAllBytes(path))), "", "");

    when(resourceService.getById(id)).thenReturn(Mono.just(new ResponsePublisher<>(getObjectResponse, byteBufferSdkPublisher)));

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
    when(resourceService.getById(id)).thenThrow(new ResourceNotFoundException("Resource is not found with id '124567'"));
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
  void shouldThrowNoSuchKeyExceptionWhenGetById() {
    long id = 124567L;
    when(resourceService.getById(id)).thenThrow(NoSuchKeyException.class);
    webTestClient.get().uri("/api/v1/resources/{id}", id)
        .accept(MediaType.APPLICATION_OCTET_STREAM)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.status").isEqualTo("NOT_FOUND")
        .jsonPath("$.message").isEqualTo("Resource file is not found");
  }

  @Test
  void shouldThrowDownloadFailedExceptionWhenGetById() {
    long id = 1L;
    SdkResponse sdkResponse = UploadPartResponse.builder().sdkHttpResponse(SdkHttpResponse.builder()
        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value()).statusText("Resource file download has failed").build()).build();
    when(resourceService.getById(id)).thenThrow(new DownloadFailedException(sdkResponse));
    webTestClient.get().uri("/api/v1/resources/{id}", id)
        .accept(MediaType.APPLICATION_OCTET_STREAM)
        .exchange()
        .expectStatus().is5xxServerError()
        .expectBody()
        .jsonPath("$.status").isEqualTo("INTERNAL_SERVER_ERROR")
        .jsonPath("$.message").value(containsString("Resource file download has failed"));
  }

  private MultiValueMap<String, Object> multiParts() throws FileNotFoundException {
    final File file = ResourceUtils.getFile("classpath:files/mpthreetest.mp3");
    final MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("file", new FileSystemResource(file));
    return parts;
  }
}
