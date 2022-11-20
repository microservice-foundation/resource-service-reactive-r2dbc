package com.epam.training.microservicefoundation.resourceservice.repository;

import java.util.Optional;

public interface JdbcRepository<Entity, ID> {
    public Entity save(Entity entity);
    public Entity update(Entity entity);
    public Optional<Entity> findById(ID primaryKey);
    public void delete(Entity entity);
}
