package com.epam.training.microservicefoundation.resourceservice.kafka.producer;

import java.util.Map;
import java.util.function.Function;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.SenderResult;

public class KafkaProducer {
  private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);
  private final ReactiveKafkaProducerTemplate<String, Object> kafkaTemplate;
  private final Map<Class<?>, Pair<String, Function<Object, ProducerRecord<String, Object>>>> publicationTopics;

  public KafkaProducer(ReactiveKafkaProducerTemplate<String, Object> kafkaTemplate,
      Map<Class<?>, Pair<String, Function<Object, ProducerRecord<String, Object>>>> publicationTopics) {
    this.kafkaTemplate = kafkaTemplate;
    this.publicationTopics = publicationTopics;
  }

  public Mono<SenderResult<Void>> publish(Object message) {
    if (publicationTopics.containsKey(message.getClass())) {
      log.info("Publishing {} message to kafka: {}", message.getClass().getName(), message);
      return kafkaTemplate.send(publicationTopics.get(message.getClass()).getSecond().apply(message));
    }
    return Mono.error(new IllegalStateException("There is no a kafka topic for this message type: " + message.getClass()));
  }
}
