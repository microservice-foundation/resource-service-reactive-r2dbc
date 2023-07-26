package com.epam.training.microservicefoundation.resourceservice.client;

import com.epam.training.microservicefoundation.resourceservice.model.dto.GetStorageDTO;
import com.epam.training.microservicefoundation.resourceservice.model.dto.StorageType;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.config.client.RetryProperties;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

public class StorageServiceClient {
  private static final Logger log = LoggerFactory.getLogger(StorageServiceClient.class);
  private static final String PATH_STORAGES = "/storages";
  private static final String PATH_ID = "/{id}";
  private static final String QUERY_PARAM_TYPE = "type";
  private final WebClient webClient;
  private final RetryProperties retryProperties;
  private final ReactiveCircuitBreaker reactiveCircuitBreaker;

  public StorageServiceClient(WebClient webClient, RetryProperties retryProperties, ReactiveCircuitBreaker reactiveCircuitBreaker) {
    this.webClient = webClient;
    this.retryProperties = retryProperties;
    this.reactiveCircuitBreaker = reactiveCircuitBreaker;
  }

  public Flux<GetStorageDTO> getByType(StorageType type) {
    log.info("Sending a request to get storages by type '{}'", type);
    return webClient.get()
        .uri(uriBuilder -> uriBuilder.path(PATH_STORAGES)
            .queryParam(QUERY_PARAM_TYPE, type)
            .build())
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToFlux(GetStorageDTO.class)
        .transform(reactiveCircuitBreaker::run)
        .retryWhen(Retry.backoff(retryProperties.getMaxAttempts(), Duration.ofMillis(retryProperties.getInitialInterval()))
            .doBeforeRetry(retrySignal -> log.info("Retrying request: attempt {}", retrySignal.totalRetriesInARow())));
  }

  public Mono<GetStorageDTO> getById(long id) {
    log.info("Sending a request to get storage by id '{}'", id);
    return webClient.get()
        .uri(uriBuilder -> uriBuilder
            .path(PATH_STORAGES)
            .path(PATH_ID)
            .build(id))
        .retrieve()
        .bodyToMono(GetStorageDTO.class)
        .transform(reactiveCircuitBreaker::run)
        .retryWhen(Retry.backoff(retryProperties.getMaxAttempts(), Duration.ofMillis(retryProperties.getInitialInterval()))
            .doBeforeRetry(retrySignal -> log.info("Retrying request: attempt {}", retrySignal.totalRetriesInARow())));
  }
}
