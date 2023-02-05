package com.epam.training.microservicefoundation.resourceservice.service.implementation;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import software.amazon.awssdk.utils.Pair;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;


public class KafkaManager {
    private static final Logger log = LoggerFactory.getLogger(KafkaManager.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Map<Class<?>, Pair<String, Function<Object, ProducerRecord<String, Object>>>> publicationTopics;

    public KafkaManager(KafkaTemplate<String, Object> kafkaTemplate, Map<Class<?>, Pair<String, Function<Object,
            ProducerRecord<String, Object>>>> publicationTopics) {

        this.kafkaTemplate = kafkaTemplate;
        this.publicationTopics = publicationTopics;
    }

    public void publish(Object message) {
        if(publicationTopics.containsKey(message.getClass())) {
            log.info("publishing {} message to kafka: {}", message.getClass().getName(), message);
            kafkaTemplate.send(publicationTopics.get(message.getClass()).right().apply(message));
        }
    }

    public void publishCallback(Object message) {
        if(publicationTopics.containsKey(message.getClass())) {
            log.info("publishing {} message to kafka: {}", message.getClass().getName(), message);

            ListenableFuture<SendResult<String, Object>> send =
                    kafkaTemplate.send(publicationTopics.get(message.getClass()).right().apply(message));

//            send.exceptionally(throwable -> {
//                log.warn("Publishing failed: {} unable to be delivered, {}", message, throwable.getMessage());
//                return null;
//            }).thenAccept(result -> log.info("Successfully published: {} message delivered with offset -{}", result,
//                    result.getRecordMetadata().offset()));
            send.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
                @Override
                public void onFailure(Throwable ex) {
                    log.warn("Publishing failed: {} unable to be delivered, {}", message, ex.getMessage());
                }

                @Override
                public void onSuccess(SendResult<String, Object> result) {
                    log.info("Successfully published: {} message delivered with offset -{}", result,
                    result.getRecordMetadata().offset());
                }
            });
        }
    }
}
