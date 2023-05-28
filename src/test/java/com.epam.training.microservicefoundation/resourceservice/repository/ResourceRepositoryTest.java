package com.epam.training.microservicefoundation.resourceservice.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.epam.training.microservicefoundation.resourceservice.config.DatasourceConfiguration;
import com.epam.training.microservicefoundation.resourceservice.model.Resource;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

  @Test
  void shouldSaveResource() {
    Resource resource = new Resource.Builder("test-file-key", "test-file-name")
        .build();

    Mono<Resource> resultMono = resourceRepository.save(resource);
    StepVerifier
        .create(resultMono)
        .assertNext(result -> {
          assertTrue(result.getId() > 0L);
          assertEquals(resource.getKey(), result.getKey());
          assertEquals(resource.getName(), result.getName());
          assertNotNull(result.getCreatedDate());
          assertNotNull(result.getLastModifiedDate());
        })
        .verifyComplete();
  }

  @Test
  void shouldThrowExceptionWhenSaveResourceWithNullPath() {
    Resource resource = new Resource.Builder(null, "test.mp3").build();
    Mono<Resource> resultMono = resourceRepository.save(resource);
    StepVerifier
        .create(resultMono)
        .expectError(DataIntegrityViolationException.class)
        .verify();
  }

  @Test
  void shouldThrowExceptionWhenSaveResourceWithExistingPathAndName() {
    Resource resource = new Resource.Builder("mess-code.mp3", "mess-code.mp3").build();
    Mono<Resource> resultMono = resourceRepository.save(resource);
    StepVerifier.create(resultMono)
        .expectError(DataIntegrityViolationException.class)
        .verify();
  }

  @Test
  void shouldFindResourceById() {
    Resource resource = new Resource.Builder("find-by-id.mp3", "find-by-id.mp3").build();
    Mono<Resource> resultMono = resourceRepository.save(resource).flatMap(result -> resourceRepository.findById(result.getId()));
    StepVerifier.create(resultMono)
        .assertNext(result -> {
          assertTrue(result.getId() > 0L);
          assertEquals(resource.getKey(), result.getKey());
          assertEquals(resource.getName(), result.getName());
          assertNotNull(result.getCreatedDate());
          assertNotNull(result.getLastModifiedDate());
        })
        .verifyComplete();
  }

  @Test
  void shouldReturnEmptyValueWhenFindResourceById() {
    long id = 439_990_999_598_833L;
    StepVerifier.create(resourceRepository.findById(id))
        .expectSubscription()
        .expectNoEvent(Duration.ofMillis(500))
        .expectComplete();
  }

  @Test
  void shouldThrowExceptionWhenUpdateResourceWithNullPath() {
    Resource resource = new Resource.Builder(null, "test.mp3").id(123L).build();
    StepVerifier.create(resourceRepository.save(resource))
        .expectError(DataIntegrityViolationException.class)
        .verify();
  }

  @Test
  void shouldThrowExceptionWhenUpdateResourceLastModifiedDateBeforeCreatedDate() {
    Resource resource = new Resource.Builder(null, null)
        .id(2L)
        .build();

    StepVerifier.create(resourceRepository.save(resource))
        .expectError(DataIntegrityViolationException.class)
        .verify();
  }


  @Test
  void shouldThrowExceptionWhenUpdateResourceWithNotExistentId() {
    Resource resource = new Resource.Builder("terminal.mp3", "terminal.mp3")
        .id(191_999_000_234L)
        .build();

    StepVerifier.create(resourceRepository.save(resource))
        .expectError(TransientDataAccessResourceException.class)
        .verify();
  }

  @Test
  void shouldDeleteResource() {
    Resource resource = new Resource.Builder("postal-code.mp3", "postal-code.mp3")
        .id(123L)
        .build();

    StepVerifier.create(resourceRepository.delete(resource))
        .expectSubscription()
        .expectNoEvent(Duration.ofMillis(500))
        .expectComplete();
  }

    @Test
    void shouldThrowExceptionWhenDeleteResourceWithNull() {
        assertThrows(IllegalArgumentException.class, () -> resourceRepository.delete(null));
    }

    @Test
    void shouldDeleteNothingWhenDeleteResourceWithNonExistentResource() {
      Resource resource = new Resource.Builder("delete-non-existent.mp3", "delete-non-existent.mp3")
          .id(18889L)
          .build();

      StepVerifier.create(resourceRepository.delete(resource))
          .expectSubscription()
          .expectNoEvent(Duration.ofMillis(500))
          .expectComplete();
    }

    @Test
    void shouldDeleteById() {
        Resource resource = new Resource.Builder(
                "to-delete-by-id.mp3", "to-delete-by-id.mp3.mp3")
                .build();

        StepVerifier.create(resourceRepository.save(resource).flatMap(result -> resourceRepository.deleteById(result.getId())))
            .expectSubscription()
            .expectNoEvent(Duration.ofMillis(500))
            .expectComplete();
    }

    @Test
    void shouldDeleteNothingWhenDeleteByNonExistentResourceId() {
      StepVerifier.create(resourceRepository.deleteById(123_144_566L))
          .expectSubscription()
          .expectNoEvent(Duration.ofMillis(500))
          .expectComplete();
    }
}
