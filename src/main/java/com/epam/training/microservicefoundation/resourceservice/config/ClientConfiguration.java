package com.epam.training.microservicefoundation.resourceservice.config;

import com.epam.training.microservicefoundation.resourceservice.client.StorageServiceClient;
import com.epam.training.microservicefoundation.resourceservice.config.properties.WebClientProperties;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.cloud.config.client.RetryProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@RefreshScope
@EnableConfigurationProperties(WebClientProperties.class)
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
  public WebClient webClient(ReactorLoadBalancerExchangeFilterFunction loadBalancerExchangeFilterFunction, WebClientProperties properties) {
    return WebClient.builder()
        .baseUrl(properties.getBaseUrl())
        .filter(loadBalancerExchangeFilterFunction)
        .clientConnector(new ReactorClientHttpConnector(httpClient(properties)))
        .build();
  }

  @Bean
  public StorageServiceClient storageServiceClient(WebClient webClient, RetryProperties retryProperties,
      ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory) {
    return new StorageServiceClient(webClient, retryProperties, circuitBreakerFactory.create("storage-service"));
  }
}
