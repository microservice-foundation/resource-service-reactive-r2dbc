package com.epam.training.microservicefoundation.resourceservice.model.exception;

public class EntityExistsException extends RuntimeException {
  public EntityExistsException(String message, Throwable error) {
    super(message, error);
  }
}
