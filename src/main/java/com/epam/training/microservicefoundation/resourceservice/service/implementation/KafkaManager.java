//package com.epam.training.microservicefoundation.resourceservice.service.implementation;
//
//import com.epam.training.microservicefoundation.resourceservice.model.ResourceProcessedEvent;
//import com.epam.training.microservicefoundation.resourceservice.model.exception.ReceiverRecordException;
//import com.epam.training.microservicefoundation.resourceservice.service.ReactiveKafkaEventListener;
//import java.time.Duration;
//import java.util.Map;
//import java.util.function.Function;
//import org.apache.kafka.clients.producer.ProducerRecord;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.boot.context.event.ApplicationStartedEvent;
//import org.springframework.cloud.config.client.RetryProperties;
//import org.springframework.context.event.EventListener;
//import org.springframework.data.util.Pair;
//import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
//import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
//import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
//import reactor.core.publisher.Mono;
//import reactor.kafka.receiver.ReceiverRecord;
//import reactor.kafka.sender.SenderResult;
//import reactor.util.retry.Retry;
//
//
//public class KafkaManager {
//  private static final Logger log = LoggerFactory.getLogger(KafkaManager.class);
//  private final ReactiveKafkaProducerTemplate<String, Object> kafkaTemplate;
//  private final Map<Class<?>, Pair<String, Function<Object, ProducerRecord<String, Object>>>> publicationTopics;
//  private final ReactiveKafkaConsumerTemplate<String, ResourceProcessedEvent> kafkaConsumerTemplate;
//  private final ReactiveKafkaEventListener<ResourceProcessedEvent> resourceProcessedEventListener;
//  private final DeadLetterPublishingRecoverer deadLetterPublishingRecoverer;
//  private final RetryProperties retryProperties;
//
//  public KafkaManager(ReactiveKafkaProducerTemplate<String, Object> kafkaTemplate, Map<Class<?>, Pair<String, Function<Object,
//      ProducerRecord<String, Object>>>> publicationTopics,
//      ReactiveKafkaConsumerTemplate<String, ResourceProcessedEvent> kafkaConsumerTemplate,
//      ReactiveKafkaEventListener<ResourceProcessedEvent> resourceProcessedEventListener,
//      DeadLetterPublishingRecoverer deadLetterPublishingRecoverer, RetryProperties retryProperties) {
//
//    this.kafkaTemplate = kafkaTemplate;
//    this.publicationTopics = publicationTopics;
//    this.kafkaConsumerTemplate = kafkaConsumerTemplate;
//    this.resourceProcessedEventListener = resourceProcessedEventListener;
//    this.deadLetterPublishingRecoverer = deadLetterPublishingRecoverer;
//    this.retryProperties = retryProperties;
//  }
//
//  public Mono<SenderResult<Void>> publish(Object message) {
//    if (publicationTopics.containsKey(message.getClass())) {
//      log.info("Publishing {} message to kafka: {}", message.getClass().getName(), message);
//      return kafkaTemplate.send(publicationTopics.get(message.getClass()).right().apply(message));
//    }
//    return Mono.error(new IllegalStateException("There is no a kafka topic for this message type: " + message.getClass()));
//  }
//
//  @EventListener(ApplicationStartedEvent.class)
//  public void subscribe() {
//    listen(kafkaConsumerTemplate, resourceProcessedEventListener);
//  }
//
//  private <T> void listen(ReactiveKafkaConsumerTemplate<String, T> consumerTemplate, ReactiveKafkaEventListener<T> eventListener) {
//    listenWithHandler(consumerTemplate, f -> handler(eventListener, f));
//  }
//
//  private <T> void listenWithHandler(ReactiveKafkaConsumerTemplate<String, T> consumerTemplate,
//      Function<ReceiverRecord<String, T>, Mono<?>> handler) {
//    consumerTemplate.receive()
//        .doOnNext(receiverRecord -> log.info("Received event message key={}, value={}", receiverRecord.key(), receiverRecord.value()))
//        .flatMap(receiverRecord -> handler.apply(receiverRecord)
//            .doOnSuccess(r -> acknowledge(receiverRecord))
//            .doOnError(e -> log.error("Exception occurred in Listener", e))
//            .onErrorMap(error -> new ReceiverRecordException(receiverRecord, error))
//            .retryWhen(Retry.backoff(retryProperties.getMaxAttempts(), Duration.ofMillis(retryProperties.getInitialInterval()))
//                .transientErrors(true))
//            .onErrorResume(error -> sendToDeadLockQueue((ReceiverRecordException) error.getCause())))
//        .repeat()
//        .subscribe();
//  }
//
//  private <T> Mono<T> sendToDeadLockQueue(ReceiverRecordException exception) {
//    deadLetterPublishingRecoverer.accept(exception.getRecord(), exception);
//    acknowledge(exception.getRecord());
//    return Mono.empty();
//  }
//
//  private void acknowledge(ReceiverRecord<?, ?> receiverRecord) {
//    receiverRecord.receiverOffset().acknowledge();
//  }
//
//  private <T> Mono<?> handler(ReactiveKafkaEventListener<T> eventListener, ReceiverRecord<String, T> receiverRecord) {
//    return eventListener.eventListened(receiverRecord.value());
//  }
//}
