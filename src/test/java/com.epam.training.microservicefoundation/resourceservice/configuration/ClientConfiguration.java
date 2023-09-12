package com.epam.training.microservicefoundation.resourceservice.configuration;

import com.epam.training.microservicefoundation.resourceservice.web.client.StorageServiceClient;
import com.epam.training.microservicefoundation.resourceservice.configuration.properties.WebClientProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.internal.InMemoryCircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.internal.InMemoryTimeLimiterRegistry;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.config.client.RetryProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@TestConfiguration
@EnableConfigurationProperties({WebClientProperties.class, RetryProperties.class, WebClientProperties.class})
@Import(value = {ReactiveResilience4JCircuitBreakerFactory.class, InMemoryCircuitBreakerRegistry.class, InMemoryTimeLimiterRegistry.class})
public class ClientConfiguration {
  private HttpClient httpClient(WebClientProperties properties) {
    return HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectionTimeout())
        .responseTimeout(Duration.ofMillis(properties.getResponseTimeout()))
        .doOnConnected(connection -> connection
            .addHandlerFirst(new ReadTimeoutHandler(properties.getReadTimeout(), TimeUnit.MILLISECONDS))
            .addHandlerLast(new WriteTimeoutHandler(properties.getWriteTimeout(), TimeUnit.MILLISECONDS)));
  }

  @Bean
  public WebClient webClient(WebClientProperties properties) {
    return WebClient.builder()
        .baseUrl(properties.getBaseUrl())
        .clientConnector(new ReactorClientHttpConnector(httpClient(properties)))
        .build();
  }

  @Bean
  public StorageServiceClient storageServiceClient(WebClient webClient, RetryProperties retryProperties,
      ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory) {
    return new StorageServiceClient(webClient, retryProperties, circuitBreakerFactory.create("storage-service"));
  }
}
