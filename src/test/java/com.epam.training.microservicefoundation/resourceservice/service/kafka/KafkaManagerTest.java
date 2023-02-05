package com.epam.training.microservicefoundation.resourceservice.service.kafka;

import com.epam.training.microservicefoundation.resourceservice.configuration.KafkaTestConfiguration;
import com.epam.training.microservicefoundation.resourceservice.configuration.KafkaTopicTestConfiguration;
import com.epam.training.microservicefoundation.resourceservice.model.ResourceRecord;
import com.epam.training.microservicefoundation.resourceservice.service.implementation.KafkaManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(value = {SpringExtension.class, KafkaExtension.class})
@ContextConfiguration(classes = {KafkaTestConfiguration.class, KafkaTopicTestConfiguration.class})
@TestPropertySource(locations = "classpath:application.properties")
class KafkaManagerTest {

    @Autowired
    private KafkaManager kafkaManager;
    @Autowired
    private FakeKafkaConsumer consumer;

    @AfterEach
    public void reset() {
        consumer.resetLatch();
    }

    @Test
    void shouldPublishMessageWithCallback() throws InterruptedException {
        ResourceRecord message = new ResourceRecord(1L);
        kafkaManager.publishCallback(message);

        boolean messageConsumed = consumer.getLatch().await(10, TimeUnit.SECONDS);
        assertTrue(messageConsumed);
        assertEquals(message.getId(), consumer.resourceRecord().getId());
    }

    @Test
    void shouldPublishMessage() throws InterruptedException {
        ResourceRecord message = new ResourceRecord(2L);
        kafkaManager.publish(message);

        boolean massageConsumed = consumer.getLatch().await(10, TimeUnit.SECONDS);
        assertTrue(massageConsumed);
        assertEquals(message.getId(), consumer.resourceRecord().getId());
    }

    @Test
    void shouldNotPublishUnexistentMessageType() throws InterruptedException {
        kafkaManager.publish(new Object());

        boolean massageConsumed = consumer.getLatch().await(10, TimeUnit.SECONDS);
        assertFalse(massageConsumed);
    }

    @Test
    void shouldNotPublishWithCallbackUnexistentMessageType() throws InterruptedException {
        kafkaManager.publishCallback(new Object());

        boolean massageConsumed = consumer.getLatch().await(10, TimeUnit.SECONDS);
        assertFalse(massageConsumed);
    }
}
