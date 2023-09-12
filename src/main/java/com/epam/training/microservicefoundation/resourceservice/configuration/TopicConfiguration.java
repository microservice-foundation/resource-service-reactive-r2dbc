package com.epam.training.microservicefoundation.resourceservice.configuration;

import com.epam.training.microservicefoundation.resourceservice.configuration.properties.TopicProperties;
import com.epam.training.microservicefoundation.resourceservice.domain.event.ResourceStagedEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.util.Pair;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@RefreshScope
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
  Map<Class<?>, Pair<String, Function<Object, ProducerRecord<String, Object>>>> publicationTopics(TopicProperties properties) {
    Map<Class<?>, Pair<String, Function<Object, ProducerRecord<String, Object>>>> map = new HashMap<>();
    map.put(ResourceStagedEvent.class, Pair.of(properties.getResourceStaging(), message ->
        new ProducerRecord<>(properties.getResourceStaging(), String.valueOf(((ResourceStagedEvent) message).getId()), message)));
    return map;
  }
}
