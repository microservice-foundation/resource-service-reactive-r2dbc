package com.epam.training.microservicefoundation.resourceservice.web.validator;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class IdQueryParamValidator implements QueryParamValidator {
  private static final String REGEX_NUMBER_SEQUENCE = "^[0-9]+(,[ 0-9]+)*$";

  @Override
  public boolean supports(Class<?> clazz) {
    return clazz.equals(String.class);
  }

  public void validate(@NonNull Object target, @NonNull QueryParamValidationErrors errors) {
    String queryParam = (String) target;
    if(!StringUtils.hasText(queryParam)) {
      errors.rejectValue( "query_param.value.length","Id query parameter value is empty.");
    } else if(!queryParam.matches(REGEX_NUMBER_SEQUENCE)) {
      errors.rejectValue("query_param.value.format", new Object[] {REGEX_NUMBER_SEQUENCE}, "Id query parameter is not numeric/sequential number.");
    }
  }
}
