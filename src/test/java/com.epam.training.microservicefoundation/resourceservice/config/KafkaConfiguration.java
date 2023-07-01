package com.epam.training.microservicefoundation.resourceservice.config;

import com.epam.training.microservicefoundation.resourceservice.config.properties.TopicProperties;
import com.epam.training.microservicefoundation.resourceservice.kafka.producer.KafkaProducer;
import com.epam.training.microservicefoundation.resourceservice.model.ResourceStagedEvent;
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

//  @Bean
//  public KafkaConsumer kafkaConsumer(DeadLetterPublishingRecoverer deadLetterPublishingRecoverer, RetryProperties retryProperties,
//      List<Pair<ReactiveKafkaConsumerTemplate<String, Object>, ReactiveKafkaEventListener<Object>>> consumerAndListeners) {
//    return new KafkaConsumer(deadLetterPublishingRecoverer, retryProperties, consumerAndListeners);
//  }

//  @Bean
//  public List<Pair<ReactiveKafkaConsumerTemplate<String, ?>, ReactiveKafkaEventListener<?>>> consumerAndListeners(
//      KafkaProperties kafkaProperties, TopicProperties topicProperties, ResourceService resourceService) {
//    return List.of(Pair.of(resourcePermanentConsumer(kafkaProperties, topicProperties), resourceProcessedEventListener(resourceService)));
//  }

//  private ResourceProcessedEventListener resourceProcessedEventListener(ResourceService service) {
//    return new ResourceProcessedEventListener(service);
//  }
//
//  private ReactiveKafkaConsumerTemplate<String, ResourceProcessedEvent> resourcePermanentConsumer(KafkaProperties kafkaProperties,
//      TopicProperties topicProperties) {
//    ReceiverOptions<String, ResourceProcessedEvent> basicReceiverOptions =
//        ReceiverOptions.create(kafkaProperties.buildConsumerProperties());
//
//    ReceiverOptions<String, ResourceProcessedEvent> receiverOptions =
//        basicReceiverOptions.subscription(Collections.singletonList(topicProperties.getResourcePermanent()));
//
//    return new ReactiveKafkaConsumerTemplate<>(receiverOptions);
//  }

  @Bean
  public ReactiveKafkaConsumerTemplate<String, ResourceStagedEvent> resourceStagingConsumer(KafkaProperties kafkaProperties,
      TopicProperties topicProperties) {
    ReceiverOptions<String, ResourceStagedEvent> basicReceiverOptions = ReceiverOptions.create(kafkaProperties.buildConsumerProperties());
    ReceiverOptions<String, ResourceStagedEvent> receiverOptions =
        basicReceiverOptions.subscription(Collections.singletonList(topicProperties.getResourceStaging()));
    return new ReactiveKafkaConsumerTemplate<>(receiverOptions);
  }

//  @Bean
//  public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaProperties kafkaProperties) {
//    return new DeadLetterPublishingRecoverer(getEventKafkaTemplate(kafkaProperties));
//  }

//  private KafkaOperations<String, Object> getEventKafkaTemplate(KafkaProperties properties) {
//    return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(properties.buildProducerProperties()));
//  }
}
