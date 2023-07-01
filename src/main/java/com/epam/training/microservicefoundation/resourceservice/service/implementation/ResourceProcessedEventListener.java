package com.epam.training.microservicefoundation.resourceservice.service.implementation;

import com.epam.training.microservicefoundation.resourceservice.model.ResourceProcessedEvent;
import com.epam.training.microservicefoundation.resourceservice.service.ReactiveKafkaEventListener;
import com.epam.training.microservicefoundation.resourceservice.service.ResourceService;
import reactor.core.publisher.Mono;

public class ResourceProcessedEventListener implements ReactiveKafkaEventListener<ResourceProcessedEvent> {
  private final ResourceService service;

  public ResourceProcessedEventListener(ResourceService service) {
    this.service = service;
  }

  @Override
  public Mono<Void> eventListened(ResourceProcessedEvent event) {
    return service.moveToPermanent(event);
  }
}
