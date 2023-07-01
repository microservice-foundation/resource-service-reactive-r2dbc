package com.epam.training.microservicefoundation.resourceservice.client;

import com.epam.training.microservicefoundation.resourceservice.model.StorageDTO;
import com.epam.training.microservicefoundation.resourceservice.model.StorageType;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  public StorageServiceClient(WebClient webClient, RetryProperties retryProperties) {
    this.webClient = webClient;
    this.retryProperties = retryProperties;
  }

  public Flux<StorageDTO> getByType(StorageType type) {
    log.info("Sending a request to get storages by type '{}'", type);
    return webClient.get()
        .uri(uriBuilder -> uriBuilder.path(PATH_STORAGES)
            .queryParam(QUERY_PARAM_TYPE, type)
            .build())
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToFlux(StorageDTO.class)
        .retryWhen(Retry.backoff(retryProperties.getMaxAttempts(), Duration.ofMillis(retryProperties.getInitialInterval()))
            .doBeforeRetry(retrySignal -> log.info("Retrying request: attempt {}", retrySignal.totalRetriesInARow())));
  }

  public Mono<StorageDTO> getById(long id) {
    log.info("Sending a request to get storage by id '{}'", id);
    return webClient.get()
        .uri(uriBuilder -> uriBuilder
            .path(PATH_STORAGES)
            .path(PATH_ID)
            .build(id))
        .retrieve()
        .bodyToMono(StorageDTO.class)
        .retryWhen(Retry.backoff(retryProperties.getMaxAttempts(), Duration.ofMillis(retryProperties.getInitialInterval()))
            .doBeforeRetry(retrySignal -> log.info("Retrying request: attempt {}", retrySignal.totalRetriesInARow())));
  }
}
