package com.epam.training.microservicefoundation.resourceservice.repository;

import com.epam.training.microservicefoundation.resourceservice.domain.ResourceNotFoundException;
import com.epam.training.microservicefoundation.resourceservice.domain.Resource;
import com.epam.training.microservicefoundation.resourceservice.domain.SongResourceRowMapper;
import com.epam.training.microservicefoundation.resourceservice.util.ResourceSQL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.Optional;

@Repository
public class ResourceRepository implements JdbcRepository<Resource, Long> {
    private final Logger log = LoggerFactory.getLogger(ResourceRepository.class);
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;
    private final ResourceSQL resourceSQL;

    @Autowired
    public ResourceRepository(NamedParameterJdbcTemplate jdbcTemplate, ResourceSQL resourceSQL) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbcInsert = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate());
        this.resourceSQL = resourceSQL;

        this.jdbcInsert.withTableName(resourceSQL.getTable());
        this.jdbcInsert.usingGeneratedKeyColumns(resourceSQL.getGeneratedKey());
    }

    @Override
    public Resource save(Resource resource) {
        log.info("Saving song resource: {}", resource);
        Number id = jdbcInsert.executeAndReturnKey(resource.toMap());
        log.debug("Song resource saved with id {}", id);

        return findById(id.longValue()).orElseThrow(() ->{
            IllegalStateException ex = new IllegalStateException("Resource was not inserted successfully");
            log.error("After song resource saved with id {}, failed finding {}", id, ex);
            return ex;
        });
    }

    @Override
    public Resource update(Resource resource) {
        log.info("Updating song resource: {}", resource);
        boolean isUpdated = jdbcTemplate.update(resourceSQL.getUpdate(), resource.toMap()) == 1;
        if(!isUpdated) {
            log.error("Updating song resource failed: {}", resource);
            throw new ResourceNotFoundException("Resource was not updated successfully, check the resource id");
        }
        log.debug("Song resource updated successfully: {}", resource);
        return findById(resource.getId()).orElseThrow(() -> {
            ResourceNotFoundException ex =
                    new ResourceNotFoundException("Resource can't be found with id=" + resource.getId());

            log.error("After song resource updated: {}, failed finding {}", resource, ex);
            return ex;
        });
    }

    @Override
    public Optional<Resource> findById(Long primaryKey) {
        log.info("Finding song resource by id {}", primaryKey);
        Optional<Resource> resource = jdbcTemplate.queryForStream(resourceSQL.getFindById(), Collections.singletonMap("id", primaryKey),
                new SongResourceRowMapper()).findFirst();
        log.debug("Resource found: {}", resource);
        return resource;
    }

    @Override
    public void delete(Resource resource) {
        log.info("Deleting song resource: {}", resource);
        if(resource == null || resource.getId() <= 0L) {
            IllegalArgumentException ex =
                    new IllegalArgumentException("Resource is null or id is zero, check the resource");

            log.error("Failed to delete resource: {} with failure {}", resource, ex);
            throw ex;
        }

        jdbcTemplate.update(resourceSQL.getDelete(), Collections.singletonMap("id", resource.getId()));
        log.debug("Song resource deleted successfully: {}", resource);
    }

    public Optional<String> findNameById(long id) {
        log.info("Finding song name by id '{}'", id);
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(resourceSQL.getFindNameById(),
                    Collections.singletonMap("id", id), String.class));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public void deleteById(long id) {
        log.info("Deleting resource by id '{}'", id);
        if(!existsById(id)) {
            IllegalArgumentException illegalArgumentException = new IllegalArgumentException(String.format("Resource " +
                    "with id '%d' is not existent", id));
            log.debug("Resource with id '{}' is not existent", id, illegalArgumentException);
            throw illegalArgumentException;
        }
        jdbcTemplate.update(resourceSQL.getDelete(), Collections.singletonMap("id", id));
        log.debug("Song resource with id '{}' deleted successfully", id);
    }

    private boolean existsById(long id) {
        log.info("Check if a resource with id '{}' exists", id);
        return Boolean.TRUE.equals(jdbcTemplate
                .queryForObject(resourceSQL.getExistsById(), Collections.singletonMap("id", id), Boolean.class));
    }
}
