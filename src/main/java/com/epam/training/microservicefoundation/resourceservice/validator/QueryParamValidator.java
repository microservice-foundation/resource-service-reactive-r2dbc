package com.epam.training.microservicefoundation.resourceservice.validator;

public interface QueryParamValidator {
  boolean supports(Class<?> clazz);
  void validate(Object object, QueryParamValidationErrors errors);
}
