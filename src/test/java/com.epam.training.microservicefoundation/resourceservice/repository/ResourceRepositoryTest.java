package com.epam.training.microservicefoundation.resourceservice.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.epam.training.microservicefoundation.resourceservice.common.PostgresExtension;
import com.epam.training.microservicefoundation.resourceservice.config.DatasourceConfiguration;
import com.epam.training.microservicefoundation.resourceservice.model.entity.Resource;
import com.epam.training.microservicefoundation.resourceservice.model.entity.ResourceStatus;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DataR2dbcTest
@ExtendWith(PostgresExtension.class)
@DirtiesContext
@ContextConfiguration(classes = DatasourceConfiguration.class)
@TestPropertySource(locations = "classpath:application.properties")
class ResourceRepositoryTest {
  @Autowired
  ResourceRepository resourceRepository;

  @AfterEach
  public void cleanUp() {
    StepVerifier.create(resourceRepository.deleteAll())
        .verifyComplete();
  }

  @ParameterizedTest
  @EnumSource(ResourceStatus.class)
  void shouldSaveResource(ResourceStatus status) {
    Resource resource = resourceByStatus(status);
    assertResourceResult(resource, resourceRepository.save(resource));
  }

  @ParameterizedTest
  @EnumSource(ResourceStatus.class)
  void shouldThrowExceptionWhenSaveResourceWithNullKey(ResourceStatus status) {
    Resource resource = Resource.builder().name("test-file-name")
        .key(null).status(status).storageId(1234L).build();

    Mono<Resource> resultMono = resourceRepository.save(resource);
    StepVerifier.create(resultMono)
        .expectError(DataIntegrityViolationException.class)
        .verify();
  }

  @ParameterizedTest
  @EnumSource(ResourceStatus.class)
  void shouldThrowExceptionWhenSaveResourceWithNullName(ResourceStatus status) {
    Resource resource = Resource.builder().name(null)
        .key("test-file-key").status(status).storageId(1234L).build();

    Mono<Resource> resultMono = resourceRepository.save(resource);
    StepVerifier.create(resultMono)
        .expectError(DataIntegrityViolationException.class)
        .verify();
  }

  @Test
  void shouldThrowExceptionWhenSaveResourceWithNullStatus() {
    Resource resource = Resource.builder().name("test-file-name")
        .key("test-file-key").status(null).storageId(1234L).build();

    Mono<Resource> resultMono = resourceRepository.save(resource);
    StepVerifier.create(resultMono)
        .expectError(DataIntegrityViolationException.class)
        .verify();
  }


  @ParameterizedTest
  @EnumSource(ResourceStatus.class)
  void shouldThrowExceptionWhenSaveResourceWithExistingName(ResourceStatus status) {
    Resource resource1 = resourceByStatus(status);
    assertResourceResult(resource1, resourceRepository.save(resource1));
    Resource resource2 = resourceByStatus(status).toBuilder().name(resource1.getName()).build();
    Mono<Resource> resultMono = resourceRepository.save(resource2);
    StepVerifier.create(resultMono)
        .expectError(DataIntegrityViolationException.class)
        .verify();
  }

  @ParameterizedTest
  @EnumSource(ResourceStatus.class)
  void shouldFindResourceById(ResourceStatus status) {
    Resource resource = resourceByStatus(status);
    assertResourceResult(resource, resourceRepository.save(resource).flatMap(result -> resourceRepository.findById(result.getId())));
  }

  @Test
  void shouldReturnEmptyValueWhenFindResourceById() {
    long id = 439_990_999_598_833L;
    StepVerifier.create(resourceRepository.findById(id))
        .expectSubscription()
        .expectNextCount(0)
        .verifyComplete();
  }

  @ParameterizedTest
  @EnumSource(ResourceStatus.class)
  void shouldThrowExceptionWhenUpdateResourceWithNullName(ResourceStatus status) {
    Resource resource1 = resourceByStatus(status);
    assertResourceResult(resource1, resourceRepository.save(resource1));

    Resource resource2 = resource1.toBuilder().name(null).build();
    StepVerifier.create(resourceRepository.save(resource2))
        .expectError(DataIntegrityViolationException.class)
        .verify();
  }

  @ParameterizedTest
  @EnumSource(ResourceStatus.class)
  void shouldThrowExceptionWhenUpdateResourceWithNullKey(ResourceStatus status) {
    Resource resource1 = resourceByStatus(status);
    assertResourceResult(resource1, resourceRepository.save(resource1));

    Resource resource2 = resource1.toBuilder().key(null).build();
    StepVerifier.create(resourceRepository.save(resource2))
        .expectError(DataIntegrityViolationException.class)
        .verify();
  }

  @ParameterizedTest
  @EnumSource(ResourceStatus.class)
  void shouldThrowExceptionWhenUpdateResourceWithNullStatus(ResourceStatus status) {
    Resource resource1 = resourceByStatus(status);
    assertResourceResult(resource1, resourceRepository.save(resource1));

    Resource resource2 = resource1.toBuilder().status(null).build();
    StepVerifier.create(resourceRepository.save(resource2))
        .expectError(DataIntegrityViolationException.class)
        .verify();
  }

  @ParameterizedTest
  @EnumSource(ResourceStatus.class)
  void shouldThrowExceptionWhenUpdateResourceWithNotExistentId(ResourceStatus status) {
    Resource resource = resourceByStatus(status).toBuilder()
        .id(123_333_546L).build();
    StepVerifier.create(resourceRepository.save(resource))
        .expectError(TransientDataAccessResourceException.class)
        .verify();
  }

  @ParameterizedTest
  @EnumSource(ResourceStatus.class)
  void shouldDeleteResource(ResourceStatus status) {
    Resource resource = resourceByStatus(status);

    StepVerifier.create(resourceRepository.save(resource).flatMap(result -> resourceRepository.delete(result)))
        .expectSubscription()
        .expectNextCount(0)
        .verifyComplete();

    StepVerifier.create(resourceRepository.existsByName(resource.getName()))
        .assertNext(Assertions::assertFalse)
        .verifyComplete();
  }

  @Test
  void shouldThrowExceptionWhenDeleteResourceWithNull() {
    assertThrows(IllegalArgumentException.class, () -> resourceRepository.delete(null));
  }

  @ParameterizedTest
  @EnumSource(ResourceStatus.class)
  void shouldDeleteNothingWhenDeleteResourceWithNonExistentResource(ResourceStatus status) {
    Resource resource = resourceByStatus(status);
    StepVerifier.create(resourceRepository.delete(resource))
        .expectSubscription()
        .expectNextCount(0)
        .verifyComplete();
  }

  @ParameterizedTest
  @EnumSource(ResourceStatus.class)
  void shouldDeleteById(ResourceStatus status) {
    Resource resource = resourceByStatus(status);

    StepVerifier.create(resourceRepository.save(resource).flatMap(result -> resourceRepository.deleteById(result.getId())))
        .expectSubscription()
        .expectNextCount(0)
        .verifyComplete();

    StepVerifier.create(resourceRepository.existsByName(resource.getName()))
        .assertNext(Assertions::assertFalse)
        .verifyComplete();
  }

  @Test
  void shouldDeleteNothingWhenDeleteByNonExistentResourceId() {
    StepVerifier.create(resourceRepository.deleteById(123_144_566L))
        .expectSubscription()
        .expectNextCount(0)
        .verifyComplete();
  }

  @ParameterizedTest
  @EnumSource(ResourceStatus.class)
  void shouldExistsByName(ResourceStatus status) {
    Resource resource = resourceByStatus(status);
    assertResourceResult(resource, resourceRepository.save(resource));

    StepVerifier.create(resourceRepository.existsByName(resource.getName()))
        .assertNext(Assertions::assertTrue)
        .verifyComplete();
  }

  @ParameterizedTest
  @EnumSource(ResourceStatus.class)
  void shouldNotExistsByName(ResourceStatus status) {
    Resource resource = resourceByStatus(status);

    StepVerifier.create(resourceRepository.existsByName(resource.getName()))
        .assertNext(Assertions::assertFalse)
        .verifyComplete();
  }

  @Test
  void shouldReturnFalseWhenCheckIfExistsByNullName() {
    StepVerifier.create(resourceRepository.existsByName(null))
        .expectSubscription()
        .assertNext(Assertions::assertFalse)
        .verifyComplete();
  }

  @Test
  void shouldReturnFalseWhenCheckIfExistsByEmptyName() {
    StepVerifier.create(resourceRepository.existsByName(""))
        .expectSubscription()
        .assertNext(Assertions::assertFalse)
        .verifyComplete();
  }

  private void assertResourceResult(Resource expected, Mono<Resource> actual) {
    StepVerifier.create(actual)
        .assertNext(result -> {
          assertTrue(result.getId() > 0L);
          assertEquals(expected.getKey(), result.getKey());
          assertEquals(expected.getName(), result.getName());
          assertEquals(expected.getStatus(), result.getStatus());
          assertEquals(expected.getStorageId(), result.getStorageId());
          assertNotNull(result.getCreatedDate());
          assertNotNull(result.getLastModifiedDate());
        })
        .verifyComplete();
  }

  private Resource resourceByStatus(ResourceStatus status) {
    Random random = new Random();
    return Resource.builder()
        .key("test-file-key-" + random.nextInt(1000))
        .name("test-file-name-" + random.nextInt(1000))
        .storageId(random.nextInt(1000))
        .status(status)
        .build();
  }
}
