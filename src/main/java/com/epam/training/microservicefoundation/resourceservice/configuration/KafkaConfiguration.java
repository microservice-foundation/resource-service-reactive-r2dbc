package com.epam.training.microservicefoundation.resourceservice.configuration;

import com.epam.training.microservicefoundation.resourceservice.service.implementation.KafkaManager;
import java.util.Map;
import java.util.function.Function;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.kafka.sender.SenderOptions;
import software.amazon.awssdk.utils.Pair;

@Configuration
@EnableKafka
@RefreshScope
public class KafkaConfiguration {

    private ReactiveKafkaProducerTemplate<String, Object> kafkaTemplate(KafkaProperties properties) {
        return new ReactiveKafkaProducerTemplate<>(SenderOptions.create(properties.buildProducerProperties()));
    }

    @Bean
    public KafkaManager kafkaManager(Map<Class<?>, Pair<String, Function<Object, ProducerRecord<String, Object>>>> publicationTopics,
        KafkaProperties properties) {
        return new KafkaManager(kafkaTemplate(properties), publicationTopics);
    }
}
