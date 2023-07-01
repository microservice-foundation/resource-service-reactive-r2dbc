package com.epam.training.microservicefoundation.resourceservice.service;

import reactor.core.publisher.Mono;

public interface ReactiveKafkaEventListener<E> {
  Mono<Void> eventListened(E event);
}
