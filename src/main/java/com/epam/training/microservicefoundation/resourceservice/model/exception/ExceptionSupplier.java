package com.epam.training.microservicefoundation.resourceservice.model.exception;

import com.epam.training.microservicefoundation.resourceservice.model.dto.StorageType;
import com.epam.training.microservicefoundation.resourceservice.validator.QueryParamValidationErrors;
import java.util.function.Supplier;
import software.amazon.awssdk.core.SdkResponse;

public class ExceptionSupplier {
  private ExceptionSupplier() {}

  public static Supplier<EntityNotFoundException> entityNotFound(Class<?> entityClass, long id) {
    return () -> new EntityNotFoundException(String.format("%s with id=%d is not found", entityClass.getSimpleName(), id));
  }

  public static Supplier<EntityNotFoundException> entityNotFound(Class<?> entityClass, StorageType storageType) {
    return () -> new EntityNotFoundException(String.format("%s with type=%s is not found", entityClass.getSimpleName(), storageType));
  }

  public static Supplier<EntityExistsException> entityAlreadyExists(Class<?> entityClass, Throwable error) {
    return () -> new EntityExistsException(String.format("%s with these parameters already exists", entityClass.getSimpleName()), error);
  }

  public static Supplier<CloudStorageException> cloudStorageProcessFailed(SdkResponse response) {
    return () -> new CloudStorageException(response);
  }

  public static Supplier<IllegalArgumentException> invalidRequest(QueryParamValidationErrors errors) {
    return () -> new IllegalArgumentException(errors.getAllErrors().toString());
  }

  public static Supplier<IllegalArgumentException> invalidRequest(Class<?> entityClass) {
    return () -> new IllegalArgumentException(String.format("Request has come with invalid %s data", entityClass.getSimpleName()));
  }

  public static Supplier<IllegalStateException> retryExhausted(Throwable error) {
    return () -> new IllegalStateException("Request retries have got exhausted", error);
  }
}
