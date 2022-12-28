package com.epam.training.microservicefoundation.resourceservice.configuration;

import com.epam.training.microservicefoundation.resourceservice.domain.ResourceRecord;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import software.amazon.awssdk.utils.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Configuration
@RefreshScope
public class TopicConfiguration {

    @Value("${kafka.topic.resources}")
    private String topicName;
    @Value("${kafka.topic.partitions.count}")
    private int partitionsCount;
    @Value("${kafka.topic.replication.factor}")
    private int replicationFactor;

    @Bean
    NewTopic resources() {
        return TopicBuilder
                .name(topicName)
                .partitions(partitionsCount)
                .replicas(replicationFactor)
                .build();
    }

    @Bean
    Map<Class<?>, Pair<String, Function<Object, ProducerRecord<String, Object>>>> publicationTopics() {
       Map<Class<?>, Pair<String, Function<Object, ProducerRecord<String, Object>>>> map = new HashMap<>();
       map.put(ResourceRecord.class, Pair.of(topicName, message -> new ProducerRecord<>(topicName,
               String.valueOf(((ResourceRecord) message).getId()), message)));

       return map;
    }
}
