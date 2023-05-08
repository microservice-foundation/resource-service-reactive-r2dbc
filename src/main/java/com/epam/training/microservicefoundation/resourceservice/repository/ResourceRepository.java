package com.epam.training.microservicefoundation.resourceservice.repository;

import com.epam.training.microservicefoundation.resourceservice.model.Resource;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceRepository extends ReactiveCrudRepository<Resource, Long> {

}
