package com.epam.training.microservicefoundation.resourceservice.domain.exception;

public class EntityNotFoundException extends RuntimeException {
  public EntityNotFoundException(String message) {
    super(message);
  }
}
