package com.epam.training.microservicefoundation.resourceservice.service.implementation;

import com.epam.training.microservicefoundation.resourceservice.domain.event.ResourceProcessedEvent;
import com.epam.training.microservicefoundation.resourceservice.service.ReactiveKafkaEventListener;
import reactor.core.publisher.Mono;

public class ResourceProcessedEventListener implements ReactiveKafkaEventListener<ResourceProcessedEvent> {
  private final PermanentResourceService permanentResourceService;

  public ResourceProcessedEventListener(PermanentResourceService permanentResourceService) {
    this.permanentResourceService = permanentResourceService;
  }

  @Override
  public Mono<Void> eventListened(ResourceProcessedEvent event) {
    return permanentResourceService.saveToPermanent(event);
  }
}
