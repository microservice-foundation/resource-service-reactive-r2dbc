package com.epam.training.microservicefoundation.resourceservice.kafka.producer;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import java.util.Map;
import java.util.function.Function;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.kafka.support.micrometer.KafkaRecordSenderContext;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.SenderResult;

public class KafkaProducer {
  private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);
  private final ReactiveKafkaProducerTemplate<String, Object> kafkaTemplate;
  private final Map<Class<?>, Pair<String, Function<Object, ProducerRecord<String, Object>>>> publicationTopics;
  private final ObservationRegistry registry;

  public KafkaProducer(ReactiveKafkaProducerTemplate<String, Object> kafkaTemplate,
      Map<Class<?>, Pair<String, Function<Object, ProducerRecord<String, Object>>>> publicationTopics, ObservationRegistry registry) {
    this.kafkaTemplate = kafkaTemplate;
    this.publicationTopics = publicationTopics;
    this.registry = registry;
  }

  public Mono<SenderResult<Void>> publish(Object message) {
    if (publicationTopics.containsKey(message.getClass())) {
      return Mono.deferContextual(contextView -> {
        log.info("Publishing {} message to kafka: {}", message.getClass().getName(), message);
        final ProducerRecord<String, Object> producerRecord = publicationTopics.get(message.getClass()).getSecond().apply(message);
        final KafkaRecordSenderContext kafkaSenderContext = new KafkaRecordSenderContext(producerRecord, null, () -> null);
        final Observation observation = Observation.createNotStarted("kafka producer", () -> kafkaSenderContext, registry)
            .parentObservation(contextView.get(ObservationThreadLocalAccessor.KEY));
        return observation.observe(() -> kafkaTemplate.send(producerRecord));
      });
    }
    return Mono.error(new IllegalStateException("There is no a kafka topic for this message type: " + message.getClass()));
  }
}
