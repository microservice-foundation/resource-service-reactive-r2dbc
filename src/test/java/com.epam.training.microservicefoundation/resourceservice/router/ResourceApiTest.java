package com.epam.training.microservicefoundation.resourceservice.router;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.epam.training.microservicefoundation.resourceservice.common.FakeFilePart;
import com.epam.training.microservicefoundation.resourceservice.common.FakeSenderResult;
import com.epam.training.microservicefoundation.resourceservice.config.WebFluxConfiguration;
import com.epam.training.microservicefoundation.resourceservice.kafka.producer.KafkaProducer;
import com.epam.training.microservicefoundation.resourceservice.mapper.DeleteResourceMapper;
import com.epam.training.microservicefoundation.resourceservice.mapper.GetResourceMapper;
import com.epam.training.microservicefoundation.resourceservice.model.ResourceFile;
import com.epam.training.microservicefoundation.resourceservice.model.dto.DeleteResourceDTO;
import com.epam.training.microservicefoundation.resourceservice.model.dto.GetResourceDTO;
import com.epam.training.microservicefoundation.resourceservice.model.dto.GetStorageDTO;
import com.epam.training.microservicefoundation.resourceservice.model.dto.StorageType;
import com.epam.training.microservicefoundation.resourceservice.model.entity.Resource;
import com.epam.training.microservicefoundation.resourceservice.model.entity.ResourceStatus;
import com.epam.training.microservicefoundation.resourceservice.model.exception.CloudStorageException;
import com.epam.training.microservicefoundation.resourceservice.model.exception.EntityNotFoundException;
import com.epam.training.microservicefoundation.resourceservice.repository.CloudStorageRepository;
import com.epam.training.microservicefoundation.resourceservice.repository.ResourceRepository;
import com.epam.training.microservicefoundation.resourceservice.service.implementation.StorageManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.DataIntegrityViolationException;
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
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.internal.async.SdkPublishers;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

@WebFluxTest
@DirtiesContext
@ContextConfiguration(classes = {WebFluxConfiguration.class})
@TestPropertySource(locations = "classpath:application.properties")
class ResourceApiTest {
  @Autowired
  private WebTestClient webTestClient;
  @MockBean
  private ResourceRepository resourceRepository;
  @MockBean
  private CloudStorageRepository cloudStorageRepository;
  @MockBean
  private StorageManager storageManager;
  @MockBean
  private DeleteResourceMapper deleteResourceMapper;
  @MockBean
  private KafkaProducer kafkaProducer;
  @MockBean
  private GetResourceMapper getResourceMapper;

  private static final String FILENAME = "mpthreetest.mp3";
  private static final Path FILE_PATH = Paths.get("src/test/resources/files/mpthreetest.mp3");
  private static final GetStorageDTO STAGING_STORAGE = new GetStorageDTO(998L, "resource-staging", "files/", StorageType.STAGING);
  private static final GetStorageDTO PERMANENT_STORAGE = new GetStorageDTO(999L, "resource-permanent", "files/", StorageType.PERMANENT);

  @Test
  void shouldReturn201WhenSaveResource() throws FileNotFoundException {
    final long id = 1L;
    when(resourceRepository.existsByName(anyString())).thenReturn(Mono.just(false));
    when(storageManager.getByType(StorageType.STAGING)).thenReturn(Mono.just(STAGING_STORAGE));
    when(cloudStorageRepository.upload(any(ResourceFile.class))).thenReturn(Mono.just(new ResourceFile()));
    final Resource savedResource = getSavedResource();
    when(resourceRepository.save(any())).thenReturn(Mono.just(savedResource));
    when(kafkaProducer.publish(any())).thenReturn(Mono.just(new FakeSenderResult<>(null, null, null)));
    when(getResourceMapper.toDto(any())).thenReturn(new GetResourceDTO(id));

    webTestClient.post().uri("/api/v1/resources/")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multiParts()))
        .exchange()
        .expectStatus().isCreated()
        .expectBody().jsonPath("$.id").isEqualTo(id);
  }

  @Test
  void shouldReturn400WhenSaveInvalidResource() throws FileNotFoundException {
    when(resourceRepository.existsByName(anyString())).thenReturn(Mono.just(true));

    webTestClient.post().uri("/api/v1/resources/")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multiParts()))
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.status").isEqualTo("BAD_REQUEST")
        .jsonPath("$.message").isEqualTo("Invalid request")
        .jsonPath("$.debugMessage").isEqualTo("Request has come with invalid FilePart data");
  }

  @Test
  void shouldReturn404WhenGetStorageInSaveResource() throws FileNotFoundException {
    when(resourceRepository.existsByName(anyString())).thenReturn(Mono.just(false));
    when(storageManager.getByType(StorageType.STAGING)).thenThrow(EntityNotFoundException.class);

    webTestClient.post().uri("/api/v1/resources/")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multiParts()))
        .exchange()
        .expectStatus().isNotFound()
        .expectBody()
        .jsonPath("$.status").isEqualTo("NOT_FOUND")
        .jsonPath("$.message").isEqualTo("Entity is not found");
  }

  @Test
  void shouldReturn500WhenGetStorageInSaveResource() throws FileNotFoundException {
    when(resourceRepository.existsByName(anyString())).thenReturn(Mono.just(false));
    when(storageManager.getByType(StorageType.STAGING)).thenThrow(new IllegalStateException("Request retries have got exhausted"));

    webTestClient.post().uri("/api/v1/resources/")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multiParts()))
        .exchange()
        .expectStatus().is5xxServerError()
        .expectBody()
        .jsonPath("$.status").isEqualTo("INTERNAL_SERVER_ERROR")
        .jsonPath("$.message").isEqualTo("Internal server error has happened")
        .jsonPath("$.debugMessage").isEqualTo("Request retries have got exhausted");
  }


  @Test
  void shouldReturn500WhenUploadResourceInSaveResource() throws FileNotFoundException {
    SdkResponse sdkResponse = UploadPartResponse.builder().sdkHttpResponse(SdkHttpResponse.builder()
        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value()).statusText("Resource file upload has failed").build()).build();

    when(resourceRepository.existsByName(anyString())).thenReturn(Mono.just(false));
    when(storageManager.getByType(StorageType.STAGING)).thenReturn(Mono.just(STAGING_STORAGE));
    when(cloudStorageRepository.upload(any(ResourceFile.class))).thenThrow(new CloudStorageException(sdkResponse));

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
  void shouldReturn400WhenInsertResourceInSaveResource() throws FileNotFoundException {
    when(resourceRepository.existsByName(anyString())).thenReturn(Mono.just(false));
    when(storageManager.getByType(StorageType.STAGING)).thenReturn(Mono.just(STAGING_STORAGE));
    when(cloudStorageRepository.upload(any(ResourceFile.class)))
        .thenReturn(Mono.just(new ResourceFile(new FakeFilePart(null, null, null), STAGING_STORAGE, "test", "test")));
    when(resourceRepository.save(any(Resource.class))).thenReturn(Mono.error(new DataIntegrityViolationException("something wrong")));
    when(cloudStorageRepository.deleteByKey(anyString(), anyString())).thenReturn(Mono.empty());

    webTestClient.post().uri("/api/v1/resources/")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multiParts()))
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.status").isEqualTo("BAD_REQUEST")
        .jsonPath("$.message").value(containsString("Entity is already existed"))
        .jsonPath("$.debugMessage").isEqualTo("Resource with these parameters already exists");
  }

  @Test
  void shouldReturn400WhenCleanUpCloudResourceInSaveResource() throws FileNotFoundException {
    SdkResponse sdkResponse = UploadPartResponse.builder().sdkHttpResponse(SdkHttpResponse.builder()
        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value()).statusText("Resource file delete has failed").build()).build();
    when(resourceRepository.existsByName(anyString())).thenReturn(Mono.just(false));
    when(storageManager.getByType(StorageType.STAGING)).thenReturn(Mono.just(STAGING_STORAGE));
    when(cloudStorageRepository.upload(any(ResourceFile.class)))
        .thenReturn(Mono.just(new ResourceFile(new FakeFilePart(null, null, null), STAGING_STORAGE, "test", "test")));
    when(resourceRepository.save(any(Resource.class))).thenReturn(Mono.error(new DataIntegrityViolationException("something wrong")));
    when(cloudStorageRepository.deleteByKey(anyString(), anyString())).thenReturn(Mono.error(new CloudStorageException(sdkResponse)));

    webTestClient.post().uri("/api/v1/resources/")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multiParts()))
        .exchange()
        .expectStatus().is5xxServerError()
        .expectBody()
        .jsonPath("$.status").isEqualTo("INTERNAL_SERVER_ERROR")
        .jsonPath("$.message").value(containsString("Resource file delete has failed"));
  }

  @Test
  void shouldReturn200WhenDeleteResourceByIds() {
    final Resource resource1 = getSavedResource();
    when(resourceRepository.findById(resource1.getId())).thenReturn(Mono.just(resource1));
    Resource resource2 = getSavedResource();
    when(resourceRepository.findById(resource2.getId())).thenReturn(Mono.just(resource2));
    when(storageManager.getById(resource1.getStorageId())).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(storageManager.getById(resource2.getStorageId())).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(cloudStorageRepository.deleteByKey(resource1.getKey(), PERMANENT_STORAGE.getBucket())).thenReturn(Mono.empty());
    when(cloudStorageRepository.deleteByKey(resource2.getKey(), PERMANENT_STORAGE.getBucket())).thenReturn(Mono.empty());
    when(resourceRepository.delete(resource1)).thenReturn(Mono.empty());
    when(resourceRepository.delete(resource2)).thenReturn(Mono.empty());
    when(deleteResourceMapper.toDto(resource1)).thenReturn(new DeleteResourceDTO(resource1.getId()));
    when(deleteResourceMapper.toDto(resource2)).thenReturn(new DeleteResourceDTO(resource2.getId()));

    webTestClient.delete().uri(uriBuilder -> uriBuilder
            .path("/api/v1/resources")
            .queryParam("id", resource1.getId() + "," + resource2.getId())
            .build())
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$[*].id").value(containsInAnyOrder(
            is((int) resource1.getId()),
            is((int) resource2.getId())));
  }

  @Test
  void shouldReturn200WhenDeleteResourceByIdsPartially() {
    final Resource resource1 = getSavedResource();
    when(resourceRepository.findById(resource1.getId())).thenReturn(Mono.just(resource1));
    Resource resource2 = getSavedResource();
    when(resourceRepository.findById(resource2.getId())).thenReturn(Mono.empty());
    when(storageManager.getById(resource1.getStorageId())).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(cloudStorageRepository.deleteByKey(resource1.getKey(), PERMANENT_STORAGE.getBucket())).thenReturn(Mono.empty());
    when(resourceRepository.delete(resource1)).thenReturn(Mono.empty());
    when(deleteResourceMapper.toDto(resource1)).thenReturn(new DeleteResourceDTO(resource1.getId()));

    webTestClient.delete().uri(uriBuilder -> uriBuilder
            .path("/api/v1/resources")
            .queryParam("id", resource1.getId() + "," + resource2.getId())
            .build())
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$[*].id").isEqualTo((int) resource1.getId());

    final Resource resource3 = getSavedResource();
    when(resourceRepository.findById(resource3.getId())).thenReturn(Mono.just(resource3));
    Resource resource4 = getSavedResource();
    when(resourceRepository.findById(resource4.getId())).thenReturn(Mono.just(resource4));
    when(storageManager.getById(resource3.getStorageId())).thenReturn(Mono.empty());
    when(storageManager.getById(resource4.getStorageId())).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(cloudStorageRepository.deleteByKey(resource4.getKey(), PERMANENT_STORAGE.getBucket())).thenReturn(Mono.empty());
    when(resourceRepository.delete(resource4)).thenReturn(Mono.empty());
    when(deleteResourceMapper.toDto(resource4)).thenReturn(new DeleteResourceDTO(resource4.getId()));

    webTestClient.delete().uri(uriBuilder -> uriBuilder
            .path("/api/v1/resources")
            .queryParam("id", resource3.getId() + "," + resource4.getId())
            .build())
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$[*].id").isEqualTo((int) resource4.getId());
  }

  @Test
  void shouldReturn200WhenFailDeleting() {
    final Long[] ids = {1L, 2L};
    when(resourceRepository.findById(ids[0])).thenReturn(Mono.empty());
    when(resourceRepository.findById(ids[1])).thenReturn(Mono.empty());

    webTestClient.delete().uri(uriBuilder -> uriBuilder
            .path("/api/v1/resources")
            .queryParam("id", ids[0] + "," + ids[1])
            .build())
        .exchange()
        .expectStatus().isOk();

    final Resource resource1 = getSavedResource();
    when(resourceRepository.findById(resource1.getId())).thenReturn(Mono.just(resource1));
    Resource resource2 = getSavedResource();
    when(resourceRepository.findById(resource2.getId())).thenReturn(Mono.just(resource2));
    when(storageManager.getById(resource1.getStorageId())).thenReturn(Mono.empty());
    when(storageManager.getById(resource2.getStorageId())).thenReturn(Mono.empty());

    webTestClient.delete().uri(uriBuilder -> uriBuilder
            .path("/api/v1/resources")
            .queryParam("id", resource1.getId() + "," + resource2.getId())
            .build())
        .exchange()
        .expectStatus().isOk();
  }

  @Test
  void shouldReturn400WhenInvalidIdQueryParam() {
    webTestClient.delete()
        .uri(uriBuilder -> uriBuilder
            .path("/api/v1/resources")
            .queryParam("id", "1,3;")
            .build())
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.status").isEqualTo("BAD_REQUEST")
        .jsonPath("$.message").isEqualTo("Invalid request");

    webTestClient.delete()
        .uri(uriBuilder -> uriBuilder
            .path("/api/v1/resources")
            .queryParam("id", "a,b")
            .build())
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.status").isEqualTo("BAD_REQUEST")
        .jsonPath("$.message").isEqualTo("Invalid request");

    webTestClient.delete()
        .uri(uriBuilder -> uriBuilder
            .path("/api/v1/resources")
            .queryParam("id", "1,")
            .build())
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.status").isEqualTo("BAD_REQUEST")
        .jsonPath("$.message").isEqualTo("Invalid request");
  }

  @Test
  void shouldReturn500WhenThrownCloudStorageExceptionInDeleteResourcesByIds() {
    final SdkResponse sdkResponse1 =
        DeleteBucketResponse.builder().sdkHttpResponse(SdkHttpResponse.builder().statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .statusText("Something bad happened during resource deletion").build()).build();
    final Resource resource1 = getSavedResource();
    when(resourceRepository.findById(resource1.getId())).thenReturn(Mono.just(resource1));
    Resource resource2 = getSavedResource();
    when(resourceRepository.findById(resource2.getId())).thenReturn(Mono.just(resource2));
    when(storageManager.getById(resource1.getStorageId())).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(storageManager.getById(resource2.getStorageId())).thenReturn(Mono.just(PERMANENT_STORAGE));
    when(cloudStorageRepository.deleteByKey(resource1.getKey(), PERMANENT_STORAGE.getBucket()))
        .thenThrow(new CloudStorageException(sdkResponse1));
    when(cloudStorageRepository.deleteByKey(resource2.getKey(), PERMANENT_STORAGE.getBucket())).thenReturn(Mono.empty());
    when(resourceRepository.delete(resource1)).thenReturn(Mono.empty());
    when(deleteResourceMapper.toDto(resource1)).thenReturn(new DeleteResourceDTO(resource1.getId()));

    webTestClient.delete().uri(uriBuilder -> uriBuilder
            .path("/api/v1/resources")
            .queryParam("id", resource1.getId() + "," + resource2.getId())
            .build())
        .exchange()
        .expectStatus().is5xxServerError()
        .expectBody()
        .jsonPath("$.status").isEqualTo("INTERNAL_SERVER_ERROR")
        .jsonPath("$.message").isEqualTo("Something bad happened during resource deletion");
  }

  @Test
  void shouldReturn200WhenGetResourceById() throws IOException {
    final Resource savedResource = getSavedResource();
    when(resourceRepository.findById(savedResource.getId())).thenReturn(Mono.just(savedResource));
    when(storageManager.getById(savedResource.getStorageId())).thenReturn(Mono.just(PERMANENT_STORAGE));

    final ResponsePublisher<GetObjectResponse> responsePublisher = getResponsePublisher();
    when(cloudStorageRepository.getByKey(savedResource.getKey(), PERMANENT_STORAGE.getBucket())).thenReturn(Mono.just(responsePublisher));

    webTestClient.get().uri("/api/v1/resources/{id}", savedResource.getId())
        .accept(MediaType.APPLICATION_OCTET_STREAM)
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader().contentLength(FILE_PATH.toFile().length());
  }

  @Test
  void shouldReturn404WhenGetById() {
    final Resource savedResource1 = getSavedResource();
    when(resourceRepository.findById(savedResource1.getId())).thenReturn(Mono.empty());

    webTestClient.get().uri("/api/v1/resources/{id}", savedResource1.getId())
        .accept(MediaType.APPLICATION_OCTET_STREAM)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.status").isEqualTo("NOT_FOUND")
        .jsonPath("$.message").isEqualTo("Entity is not found")
        .jsonPath("$.debugMessage").isEqualTo(String.format("Resource with id=%d is not found", savedResource1.getId()));

    final Resource savedResource2 = getSavedResource();
    when(resourceRepository.findById(savedResource2.getId())).thenReturn(Mono.just(savedResource2));
    when(storageManager.getById(savedResource2.getStorageId())).thenReturn(Mono.empty());

    webTestClient.get().uri("/api/v1/resources/{id}", savedResource2.getId())
        .accept(MediaType.APPLICATION_OCTET_STREAM)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.status").isEqualTo("NOT_FOUND")
        .jsonPath("$.message").isEqualTo("Entity is not found")
        .jsonPath("$.debugMessage").isEqualTo(String.format("Resource with id=%d is not found", savedResource2.getId()));

    final Resource savedResource3 = getSavedResource();
    when(resourceRepository.findById(savedResource3.getId())).thenReturn(Mono.just(savedResource3));
    when(storageManager.getById(savedResource3.getStorageId())).thenReturn(Mono.just(PERMANENT_STORAGE));

    when(cloudStorageRepository.getByKey(savedResource3.getKey(), PERMANENT_STORAGE.getBucket())).thenReturn(Mono.empty());

    webTestClient.get().uri("/api/v1/resources/{id}", savedResource3.getId())
        .accept(MediaType.APPLICATION_OCTET_STREAM)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.status").isEqualTo("NOT_FOUND")
        .jsonPath("$.message").isEqualTo("Entity is not found")
        .jsonPath("$.debugMessage").isEqualTo(String.format("Resource with id=%d is not found", savedResource3.getId()));

    final Resource savedResource4 = getSavedResource();
    when(resourceRepository.findById(savedResource4.getId())).thenReturn(Mono.just(savedResource4));
    when(storageManager.getById(savedResource4.getStorageId())).thenReturn(Mono.just(PERMANENT_STORAGE));

    when(cloudStorageRepository.getByKey(savedResource4.getKey(), PERMANENT_STORAGE.getBucket())).thenThrow(NoSuchKeyException.class);

    webTestClient.get().uri("/api/v1/resources/{id}", savedResource4.getId())
        .accept(MediaType.APPLICATION_OCTET_STREAM)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.status").isEqualTo("NOT_FOUND")
        .jsonPath("$.message").isEqualTo("Resource key is not found");
  }


  @Test
  void shouldThrowDownloadFailedExceptionWhenGetById() {
    final Resource savedResource = getSavedResource();
    when(resourceRepository.findById(savedResource.getId())).thenReturn(Mono.just(savedResource));
    when(storageManager.getById(savedResource.getStorageId())).thenReturn(Mono.just(PERMANENT_STORAGE));

    final SdkResponse sdkResponse = UploadPartResponse.builder().sdkHttpResponse(SdkHttpResponse.builder()
        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value()).statusText("Resource file download has failed").build()).build();
    when(cloudStorageRepository.getByKey(savedResource.getKey(), PERMANENT_STORAGE.getBucket())).thenThrow(
        new CloudStorageException(sdkResponse));

    webTestClient.get().uri("/api/v1/resources/{id}", savedResource.getId())
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

  private static final Random RANDOM = new Random();

  private Resource getSavedResource() {
    int id = RANDOM.nextInt(1000);
    return Resource.builder().status(ResourceStatus.PROCESSED).key(UUID.randomUUID().toString()).id(id)
        .storageId(RANDOM.nextInt(1000)).name("test" + id).build();
  }

  private ResponsePublisher<GetObjectResponse> getResponsePublisher() throws IOException {
    final GetObjectResponse getObjectResponse = GetObjectResponse.builder().contentType(MediaType.MULTIPART_FORM_DATA.toString())
        .contentLength(FILE_PATH.toFile().length()).build();
    final SdkPublisher<ByteBuffer> byteBufferSdkPublisher =
        SdkPublishers.envelopeWrappedPublisher(Mono.just(ByteBuffer.wrap(Files.readAllBytes(FILE_PATH))), "", "");
    return new ResponsePublisher<>(getObjectResponse, byteBufferSdkPublisher);
  }
}
