package com.epam.training.microservicefoundation.resourceservice.service;

public interface Validator<I> {
    boolean validate(I input);
}
