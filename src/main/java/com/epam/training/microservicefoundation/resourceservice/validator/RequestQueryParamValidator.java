package com.epam.training.microservicefoundation.resourceservice.validator;

import com.epam.training.microservicefoundation.resourceservice.model.exception.ExceptionSupplier;
import java.util.Optional;
import reactor.util.Logger;
import reactor.util.Loggers;

public class RequestQueryParamValidator {
  private static final Logger log = Loggers.getLogger(RequestQueryParamValidator.class);
  private final QueryParamValidator idQueryParamValidator;

  public RequestQueryParamValidator(QueryParamValidator idQueryParamValidator) {
    this.idQueryParamValidator = idQueryParamValidator;
  }

  public final String validateQueryParam(Optional<String> paramValue, String queryParamName) {
    log.info("Validating request parameter value.");
    QueryParamValidationErrors errors = new QueryParamValidationErrors(queryParamName);
    paramValue.ifPresentOrElse(value -> this.idQueryParamValidator.validate(value, errors),
        () -> errors.rejectValue("query_param",  "Query param must not be empty/null."));

    if (errors.getAllErrors().isEmpty()) {
      String value = paramValue.get();
      log.debug("Request param value is validated successfully: {}", value);
      return value;
    } else {
      throw ExceptionSupplier.invalidRequest(errors).get();
    }
  }
}
