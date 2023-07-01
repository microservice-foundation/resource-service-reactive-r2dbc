package com.epam.training.microservicefoundation.resourceservice.model.exception;

public final class StorageNotFoundException extends RuntimeException {
  public StorageNotFoundException(String message) {
    super(message);
  }

  public StorageNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
