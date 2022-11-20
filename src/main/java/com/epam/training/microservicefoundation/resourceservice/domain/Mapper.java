package com.epam.training.microservicefoundation.resourceservice.domain;

public interface Mapper<I, O> {
    O mapToRecord(I resource);

}
