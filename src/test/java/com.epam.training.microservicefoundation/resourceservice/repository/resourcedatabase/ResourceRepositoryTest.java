package com.epam.training.microservicefoundation.resourceservice.repository.resourcedatabase;

import com.epam.training.microservicefoundation.resourceservice.domain.ResourceNotFoundException;
import com.epam.training.microservicefoundation.resourceservice.domain.Resource;
import com.epam.training.microservicefoundation.resourceservice.repository.ResourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ExtendWith(PostgresExtension.class)
@DirtiesContext
@Sql(value = {"/sql/drop-schema.sql", "/sql/create-schema.sql"})
@Sql(value = "/sql/data.sql")
class ResourceRepositoryTest {
    @Autowired
    ResourceRepository resourceRepository;

    @Test
    void shouldSaveResource() {
        String path = "song/example.mp3";
        LocalDateTime createdDate = LocalDateTime.now();
        Resource resource = new Resource.Builder(path, path.split("/")[1])
                                    .createdDate(createdDate)
                                    .build();
        Resource result = resourceRepository.save(resource);

        assertNotNull(result);
        assertTrue(result.getId() > 0L);
        assertEquals(path, result.getPath());
        assertEquals(createdDate, result.getCreatedDate());
    }

    @Test
    void shouldThrowExceptionWhenSaveResourceWithNullPath() {
        Resource resource = new Resource.Builder(null, "test.mp3")
                .createdDate(LocalDateTime.now()).build();
        assertThrows(DataIntegrityViolationException.class, ()-> resourceRepository.save(resource));
    }

    @Test
    void shouldThrowExceptionWhenSaveResourceWithExistingPathAndName() {
        Resource resource = new Resource.Builder("example/mess-code.mp3", "mess-code.mp3")
                .createdDate(LocalDateTime.now()).build();

        assertThrows(DuplicateKeyException.class, ()-> resourceRepository.save(resource));
    }

    @Test
    void shouldFindResourceById() {
        long id = 1L;
        Optional<Resource> result = resourceRepository.findById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
    }

    @Test
    void shouldReturnEmptyValueWhenFindResourceById() {
        long id = 439_990_999_598_833L;
        Optional<Resource> result = resourceRepository.findById(id);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldUpdateResourcePathWhenUpdateResource() {
        Resource resource = new Resource.Builder("example/update-mess-code.mp3", "updated-mess-code.mp3")
                .id(1L)
                .lastModifiedDate(LocalDateTime.now()).build();

        Resource result = resourceRepository.update(resource);

        assertNotNull(result);
        assertEquals(resource.getId(), result.getId());
        assertEquals(resource.getPath(), result.getPath());
        assertEquals(resource.getLastModifiedDate(), result.getLastModifiedDate());
        assertNotNull(result.getCreatedDate());
    }

    @Test
    void shouldThrowExceptionWhenUpdateResourceWithNullPath() {
        Resource resource = new Resource.Builder( null, "test.mp3")
                .id(2L)
                .lastModifiedDate(LocalDateTime.now()).build();
        assertThrows(DataIntegrityViolationException.class, () -> resourceRepository.update(resource));
    }

    @Test
    void shouldThrowExceptionWhenUpdateResourceLastModifiedDateBeforeCreatedDate() {
        Resource resource = new Resource.Builder( null, null)
                .id(2L)
                .lastModifiedDate(LocalDateTime.of(2022, 10, 5, 0, 0, 0))
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> resourceRepository.update(resource));
    }


    @Test
    void shouldThrowExceptionWhenUpdateResourceWithNotExistentId() {
        Resource resource = new Resource.Builder("example/terminal.mp3", "terminal.mp3")
                .id(191_999_000_234L)
                .lastModifiedDate(LocalDateTime.now()).build();

        assertThrows(ResourceNotFoundException.class, () -> resourceRepository.update(resource));
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
        assertThrows(IllegalArgumentException.class, () -> resourceRepository.delete(null));
    }

    @Test
    void shouldThrowExceptionWhenDeleteResourceWithZeroId() {
        Resource resource = new Resource.Builder(
                "example/zero-id.mp3", "zero-id.mp3")
                .build();
        assertThrows(IllegalArgumentException.class, ()-> resourceRepository.delete(resource));
    }

    @Test
    void shouldFindNameById() {
        Optional<String> name = resourceRepository.findNameById(1L);
        assertNotNull(name);
        assertTrue(name.isPresent());
    }

    @Test
    void shouldReturnEmptyValueWhenFindNameById() {
        Optional<String> name = resourceRepository.findNameById(439_990_999_598_833L);
        assertNotNull(name);
        assertTrue(name.isEmpty());
    }

    @Test
    void shouldDeleteById() {
        long id = 1L;
        resourceRepository.deleteById(id);

        Optional<Resource> result = resourceRepository.findById(id);
        assertTrue(result.isEmpty());
    }
}
