package com.epam.training.microservicefoundation.resourceservice.service.kafka;

import com.epam.training.microservicefoundation.resourceservice.configuration.KafkaConfiguration;
import com.epam.training.microservicefoundation.resourceservice.configuration.TopicConfiguration;
import com.epam.training.microservicefoundation.resourceservice.domain.ResourceRecord;
import com.epam.training.microservicefoundation.resourceservice.service.implementation.KafkaManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {KafkaConfiguration.class, TopicConfiguration.class})
@ExtendWith(KafkaExtension.class)
class KafkaManagerTest {

    @Autowired
    private KafkaManager kafkaManager;
    @Value("${kafka.topic.resources}")
    private String topicName;

    @Test
    void shouldPublishMessageWithCallback() {
        kafkaManager.publishCallback(new ResourceRecord(1L));
    }

    @Test
    void shouldPublishMessage() {
        kafkaManager.publish(new ResourceRecord(2L));
    }
}
