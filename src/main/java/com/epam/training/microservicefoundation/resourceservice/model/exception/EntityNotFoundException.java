package com.epam.training.microservicefoundation.resourceservice.model.exception;

public class EntityNotFoundException extends RuntimeException {
  public EntityNotFoundException(String message) {
    super(message);
  }
}
