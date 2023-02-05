package com.epam.training.microservicefoundation.resourceservice.model;

public interface Mapper<I, O> {
    O mapToRecord(I resource);

}
