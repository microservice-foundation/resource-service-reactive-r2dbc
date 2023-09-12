package com.epam.training.microservicefoundation.resourceservice.repository;

import com.epam.training.microservicefoundation.resourceservice.domain.entity.Resource;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ResourceRepository extends ReactiveCrudRepository<Resource, Long> {
  Mono<Boolean> existsByName(String name);
}
