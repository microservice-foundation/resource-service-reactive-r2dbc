package com.epam.training.microservicefoundation.resourceservice.configuration;

import com.epam.training.microservicefoundation.resourceservice.api.ResourceExceptionHandler;
import com.epam.training.microservicefoundation.resourceservice.api.ResourceHandler;
import com.epam.training.microservicefoundation.resourceservice.api.ResourceRouter;
import com.epam.training.microservicefoundation.resourceservice.model.Mapper;
import com.epam.training.microservicefoundation.resourceservice.model.Resource;
import com.epam.training.microservicefoundation.resourceservice.model.ResourceRecord;
import com.epam.training.microservicefoundation.resourceservice.repository.CloudStorageRepository;
import com.epam.training.microservicefoundation.resourceservice.repository.ResourceRepository;
import com.epam.training.microservicefoundation.resourceservice.service.ResourceService;
import com.epam.training.microservicefoundation.resourceservice.service.implementation.KafkaManager;
import com.epam.training.microservicefoundation.resourceservice.service.implementation.ResourceServiceImpl;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.multipart.DefaultPartHttpMessageReader;
import org.springframework.http.codec.multipart.MultipartHttpMessageReader;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@TestConfiguration
@EnableWebFlux
@Import(value = {ResourceRouter.class, ResourceHandler.class})
@EnableConfigurationProperties(WebProperties.class)
public class WebFluxConfiguration implements WebFluxConfigurer {

  // @Order(Ordered.HIGHEST_PRECEDENCE) on ExceptionHandler class in Spring is used to define the order in which multiple exception handler classes get executed.
  // When multiple exception handler classes are present, the one with the highest precedence will be executed first.
  // The Ordered.HIGHEST_PRECEDENCE constant is used to set the order of the bean to the highest possible value. This ensures that the exception handler gets executed before any other error handling method, even the default Spring error handler.
  // This can be important if there are multiple exception handlers present and you want to ensure that a specific handler gets executed before any other.
  @Order(Ordered.HIGHEST_PRECEDENCE)
  @Bean
  public ResourceExceptionHandler resourceExceptionHandler(WebProperties webProperties, ApplicationContext applicationContext,
      ServerCodecConfigurer configurer) {
    ResourceExceptionHandler exceptionHandler =
        new ResourceExceptionHandler(new DefaultErrorAttributes(), webProperties.getResources(), applicationContext);

    exceptionHandler.setMessageReaders(configurer.getReaders());
    exceptionHandler.setMessageWriters(configurer.getWriters());
    return exceptionHandler;
  }

  @Override
  public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
    // https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/codec/multipart/DefaultPartHttpMessageReader.html#setMaxInMemorySize(int)
    DefaultPartHttpMessageReader partReader = new DefaultPartHttpMessageReader();
    partReader.setMaxParts(3);

    // Configure the maximum amount of disk space allowed for file parts
    partReader.setMaxDiskUsagePerPart(30L * 10000L * 1024L); // 307,2 MO
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
  public ResourceService service(CloudStorageRepository cloudStorageRepository, ResourceRepository resourceRepository,
      Mapper<Resource, ResourceRecord> mapper, KafkaManager kafkaManager) {

    return new ResourceServiceImpl(resourceRepository, cloudStorageRepository, mapper, kafkaManager);
  }
}
