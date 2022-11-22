package com.epam.training.microservicefoundation.resourceservice.repository.resourcedatabase;

import com.epam.training.microservicefoundation.resourceservice.domain.Resource;
import com.epam.training.microservicefoundation.resourceservice.repository.ResourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ExtendWith(PostgresExtension.class)
@DirtiesContext
@Sql(value = "/sql/data.sql")
class ResourceRepositoryTest {
    @Autowired
    ResourceRepository resourceRepository;

    @Test
    void shouldSaveResource() {
        String path = "song/example.mp3";
        LocalDateTime createdDate = LocalDateTime.now();
        Resource resource = new Resource.Builder(path, path.split("/")[1])
                                    .build();
        Resource result = resourceRepository.persist(resource);

        assertNotNull(result);
        assertTrue(result.getId() > 0L);
        assertEquals(path, result.getPath());
        assertTrue(createdDate.isBefore(result.getCreatedDate()));
    }

    @Test
    void shouldThrowExceptionWhenSaveResourceWithNullPath() {
        Resource resource = new Resource.Builder(null, "test.mp3").build();
        assertThrows(DataIntegrityViolationException.class, ()-> resourceRepository.persist(resource));
    }

    @Test
    void shouldThrowExceptionWhenSaveResourceWithExistingPathAndName() {
        Resource resource = new Resource.Builder("example/mess-code.mp3", "mess-code.mp3").build();

        assertThrows(DataIntegrityViolationException.class, () -> resourceRepository.persistAndFlush(resource));
    }

    @Test
    void shouldFindResourceById() {
        Resource resource = new Resource.Builder("test/find-by-id.mp3", "find-by-id.mp3").build();
        Resource result = resourceRepository.persistAndFlush(resource);
        Optional<Resource> optionalResourceResult = resourceRepository.findById(resource.getId());

        assertTrue(optionalResourceResult.isPresent());
        assertEquals(result.getId(), optionalResourceResult.get().getId());
    }

    @Test
    void shouldReturnEmptyValueWhenFindResourceById() {
        long id = 439_990_999_598_833L;
        Optional<Resource> result = resourceRepository.findById(id);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenUpdateResourceWithNullPath() {
        Resource resource = new Resource.Builder( null, "test.mp3")
                .id(2L)
                .build();
        assertThrows(DataIntegrityViolationException.class, () -> resourceRepository.updateAndFlush(resource));
    }

    @Test
    void shouldThrowExceptionWhenUpdateResourceLastModifiedDateBeforeCreatedDate() {
        Resource resource = new Resource.Builder( null, null)
                .id(2L)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> resourceRepository.updateAndFlush(resource));
    }


    @Test
    void shouldThrowExceptionWhenUpdateResourceWithNotExistentId() {
        Resource resource = new Resource.Builder("example/terminal.mp3", "terminal.mp3")
                .id(191_999_000_234L)
                .build();

        assertThrows(ObjectOptimisticLockingFailureException.class, () -> resourceRepository.updateAndFlush(resource));
    }

    @Test
    void shouldDeleteResource() {
        Resource resource = new Resource.Builder( "example/mess-code.mp3", "mess-code.mp3")
                .id(1L)
                .build();
        resourceRepository.delete(resource);

        Optional<Resource> result = resourceRepository.findById(resource.getId());
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenDeleteResourceWithNull() {
        assertThrows(InvalidDataAccessApiUsageException.class, () -> resourceRepository.delete(null));
    }

    @Test
    void shouldDeleteById() {
        Resource resource = new Resource.Builder(
                "example/to-delete.mp3", "to-delete.mp3")
                .build();
        Resource result = resourceRepository.persistAndFlush(resource);
        resourceRepository.deleteById(result.getId());

        boolean isExistent = resourceRepository.existsById(result.getId());
        assertFalse(isExistent);
    }
}
