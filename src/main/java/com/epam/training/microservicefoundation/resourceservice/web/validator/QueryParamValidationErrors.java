package com.epam.training.microservicefoundation.resourceservice.web.validator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class QueryParamValidationErrors implements Serializable {
  private static final long serialVersionUID = 2023_07_15_15_54L;
  private final List<QueryParamError> errors = new ArrayList<>();
  private final String queryParam;

  public QueryParamValidationErrors(String queryParam) {
    this.queryParam = queryParam;
  }

  public void rejectValue(String errorCode, String defaultMessage) {
    addError(new QueryParamError(getQueryParam(), errorCode, null, defaultMessage));
  }

  public void rejectValue(String errorCode, Object[] errorArgs, String defaultMessage) {
    addError(new QueryParamError(getQueryParam(), errorCode, errorArgs, defaultMessage));
  }

  private String getQueryParam() {
    return this.queryParam;
  }

  private void addError(QueryParamError error) {
    this.errors.add(error);
  }

  public List<QueryParamError> getAllErrors() {
    return Collections.unmodifiableList(errors);
  }

  private static final class QueryParamError {
    private final String paramName;
    private final String errorCode;
    private final Object[] errorArgs;
    private final String defaultMessage;

    public QueryParamError(String paramName, String errorCode, Object[] errorArgs, String defaultMessage) {
      this.paramName = paramName;
      this.errorCode = errorCode;
      this.errorArgs = errorArgs;
      this.defaultMessage = defaultMessage;
    }

    public String getParamName() {
      return paramName;
    }

    public String getErrorCode() {
      return errorCode;
    }

    public Object[] getErrorArgs() {
      return errorArgs;
    }

    public String getDefaultMessage() {
      return defaultMessage;
    }

    @Override
    public String toString() {
      return "QueryParamError{" +
          "paramName='" + paramName + '\'' +
          ", errorCode='" + errorCode + '\'' +
          ", errorArgs=" + Arrays.toString(errorArgs) +
          ", defaultMessage='" + defaultMessage + '\'' +
          '}';
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getName());
    sb.append(": ").append(errors.size()).append(" errors");
    for (QueryParamError error : errors) {
      sb.append('\n').append(error);
    }
    return sb.toString();
  }
}
