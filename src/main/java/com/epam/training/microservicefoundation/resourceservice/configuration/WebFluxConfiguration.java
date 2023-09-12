package com.epam.training.microservicefoundation.resourceservice.configuration;

import com.epam.training.microservicefoundation.resourceservice.web.handler.ResourceExceptionHandler;
import com.epam.training.microservicefoundation.resourceservice.web.validator.QueryParamValidator;
import com.epam.training.microservicefoundation.resourceservice.web.validator.RequestQueryParamValidator;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.cloud.config.client.RetryProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.multipart.DefaultPartHttpMessageReader;
import org.springframework.http.codec.multipart.MultipartHttpMessageReader;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
@EnableWebFlux
@EnableConfigurationProperties({WebProperties.class, RetryProperties.class})
public class WebFluxConfiguration implements WebFluxConfigurer {

  @Order(Ordered.HIGHEST_PRECEDENCE)
  @Bean
  public ResourceExceptionHandler resourceExceptionHandler(WebProperties webProperties, ApplicationContext applicationContext,
      ServerCodecConfigurer configurer, ErrorAttributeOptions errorAttributeOptions) {
    ResourceExceptionHandler exceptionHandler =
        new ResourceExceptionHandler(new DefaultErrorAttributes(), webProperties.getResources(), applicationContext, errorAttributeOptions);

    exceptionHandler.setMessageReaders(configurer.getReaders());
    exceptionHandler.setMessageWriters(configurer.getWriters());
    return exceptionHandler;
  }

  @Override
  public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
    DefaultPartHttpMessageReader partReader = new DefaultPartHttpMessageReader();
    partReader.setMaxParts(3);

    // Configure the maximum amount of disk space allowed for file parts
    partReader.setMaxDiskUsagePerPart(30L * 10000L * 1024L);
    partReader.setEnableLoggingRequestDetails(true);

    MultipartHttpMessageReader multipartReader = new MultipartHttpMessageReader(partReader);
    multipartReader.setEnableLoggingRequestDetails(true);
    configurer.defaultCodecs().multipartReader(multipartReader);

    /*
    Configure the maximum amount of memory allowed per part. When the limit is exceeded:
    file parts are written to a temporary file.
    non-file parts are rejected with DataBufferLimitException.
    */
    configurer.defaultCodecs().maxInMemorySize(512 * 1024);
  }

  @Bean
  public RequestQueryParamValidator requestQueryParamValidator(QueryParamValidator idQueryParamValidator) {
    return new RequestQueryParamValidator(idQueryParamValidator);
  }

  @Bean
  public ErrorAttributeOptions errorAttributeOptions(
      @Value("${server.error.include-message}") ErrorProperties.IncludeAttribute includeMessage,
      @Value("${server.error.include-stacktrace}") ErrorProperties.IncludeStacktrace includeStackTrace) {
    Set<ErrorAttributeOptions.Include>
        includes = new HashSet<>(Set.of(ErrorAttributeOptions.Include.EXCEPTION, ErrorAttributeOptions.Include.BINDING_ERRORS));
    if (includeMessage.equals(ErrorProperties.IncludeAttribute.ALWAYS)) {
      includes.add(ErrorAttributeOptions.Include.MESSAGE);
    }
    if (includeStackTrace.equals(ErrorProperties.IncludeStacktrace.ALWAYS)) {
      includes.add(ErrorAttributeOptions.Include.STACK_TRACE);
    }
    return ErrorAttributeOptions.of(includes);
  }
}
