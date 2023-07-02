package com.epam.training.microservicefoundation.resourceservice.config;

import com.epam.training.microservicefoundation.resourceservice.config.properties.TopicProperties;
import com.epam.training.microservicefoundation.resourceservice.model.ResourceStagedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.util.Pair;
import org.springframework.kafka.config.TopicBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@TestConfiguration
@EnableConfigurationProperties(TopicProperties.class)
public class TopicConfiguration {

  @Bean
  NewTopic resourceStaging(TopicProperties properties) {
    return TopicBuilder
        .name(properties.getResourceStaging())
        .partitions(properties.getProperties().getPartitionCount())
        .replicas(properties.getProperties().getReplicationFactor())
        .build();
  }

  @Bean
  public Map<Class<?>, Pair<String, Function<Object, ProducerRecord<String, Object>>>> publicationTopics(TopicProperties properties) {
    Map<Class<?>, Pair<String, Function<Object, ProducerRecord<String, Object>>>> map = new HashMap<>();
    map.put(ResourceStagedEvent.class, Pair.of(properties.getResourceStaging(), message ->
        new ProducerRecord<>(properties.getResourceStaging(), String.valueOf(((ResourceStagedEvent) message).getId()), message)));
    return map;
  }
}
