package com.epam.training.microservicefoundation.resourceservice.kafka.consumer;

import com.epam.training.microservicefoundation.resourceservice.model.ResourceProcessedEvent;
import com.epam.training.microservicefoundation.resourceservice.model.exception.ReceiverRecordException;
import com.epam.training.microservicefoundation.resourceservice.service.ReactiveKafkaEventListener;
import com.epam.training.microservicefoundation.resourceservice.service.implementation.ResourceProcessedEventListener;
import java.time.Duration;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.cloud.config.client.RetryProperties;
import org.springframework.context.event.EventListener;
import org.springframework.data.util.Pair;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.util.retry.Retry;

public class KafkaConsumer {
  private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);
  private final DeadLetterPublishingRecoverer deadLetterPublishingRecoverer;
  private final RetryProperties retryProperties;
  private final Pair<ReactiveKafkaConsumerTemplate<String, ResourceProcessedEvent>, ResourceProcessedEventListener>
      resourceProcessedEventListener;

  public KafkaConsumer(DeadLetterPublishingRecoverer deadLetterPublishingRecoverer, RetryProperties retryProperties,
      Pair<ReactiveKafkaConsumerTemplate<String, ResourceProcessedEvent>, ResourceProcessedEventListener> resourceProcessedEventListener) {
    this.resourceProcessedEventListener = resourceProcessedEventListener;
    this.deadLetterPublishingRecoverer = deadLetterPublishingRecoverer;
    this.retryProperties = retryProperties;
  }

  @EventListener(ApplicationStartedEvent.class)
  public void subscribe() {
    listen(resourceProcessedEventListener.getFirst(), resourceProcessedEventListener.getSecond());
  }

  private <T> void listen(ReactiveKafkaConsumerTemplate<String, T> consumerTemplate, ReactiveKafkaEventListener<T> eventListener) {
    listenWithHandler(consumerTemplate, f -> handler(eventListener, f));
  }

  private <T> void listenWithHandler(ReactiveKafkaConsumerTemplate<String, T> consumerTemplate,
      Function<ReceiverRecord<String, T>, Mono<?>> handler) {
    consumerTemplate.receive()
        .doOnNext(receiverRecord -> log.info("Received event message key={}, value={}", receiverRecord.key(), receiverRecord.value()))
        .flatMap(receiverRecord -> handler.apply(receiverRecord)
            .doOnSuccess(r -> acknowledge(receiverRecord))
            .doOnError(e -> log.error("Exception occurred in Listener", e))
            .onErrorMap(error -> new ReceiverRecordException(receiverRecord, error))
            .retryWhen(Retry.backoff(retryProperties.getMaxAttempts(), Duration.ofMillis(retryProperties.getInitialInterval()))
                .transientErrors(true))
            .onErrorResume(error -> sendToDeadLockQueue((ReceiverRecordException) error.getCause())))
        .repeat()
        .subscribe();
  }

  private <T> Mono<T> sendToDeadLockQueue(ReceiverRecordException exception) {
    deadLetterPublishingRecoverer.accept(exception.getRecord(), exception);
    acknowledge(exception.getRecord());
    return Mono.empty();
  }

  private void acknowledge(ReceiverRecord<?, ?> receiverRecord) {
    receiverRecord.receiverOffset().acknowledge();
  }

  private <T> Mono<?> handler(ReactiveKafkaEventListener<T> eventListener, ReceiverRecord<String, T> receiverRecord) {
    return eventListener.eventListened(receiverRecord.value());
  }
}
