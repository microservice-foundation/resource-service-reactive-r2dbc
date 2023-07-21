package com.epam.training.microservicefoundation.resourceservice.config;

import com.epam.training.microservicefoundation.resourceservice.config.properties.TopicProperties;
import com.epam.training.microservicefoundation.resourceservice.kafka.producer.KafkaProducer;
import com.epam.training.microservicefoundation.resourceservice.model.event.ResourceStagedEvent;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.util.Pair;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.SenderOptions;

@TestConfiguration
@EnableKafka
public class KafkaConfiguration {

  @Bean
  public ReactiveKafkaProducerTemplate<String, Object> kafkaProducerTemplate(KafkaProperties properties) {
    return new ReactiveKafkaProducerTemplate<>(SenderOptions.create(properties.buildProducerProperties()));
  }

  @Bean
  public KafkaProducer kafkaProducer(ReactiveKafkaProducerTemplate<String, Object> kafkaProducerTemplate,
      Map<Class<?>, Pair<String, Function<Object, ProducerRecord<String, Object>>>> publicationTopics) {
    return new KafkaProducer(kafkaProducerTemplate, publicationTopics);
  }

  @Bean
  public ReactiveKafkaConsumerTemplate<String, ResourceStagedEvent> resourceStagingConsumer(KafkaProperties kafkaProperties,
      TopicProperties topicProperties) {
    ReceiverOptions<String, ResourceStagedEvent> basicReceiverOptions = ReceiverOptions.create(kafkaProperties.buildConsumerProperties());
    ReceiverOptions<String, ResourceStagedEvent> receiverOptions =
        basicReceiverOptions.subscription(Collections.singletonList(topicProperties.getResourceStaging()));
    return new ReactiveKafkaConsumerTemplate<>(receiverOptions);
  }
}
