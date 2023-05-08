package com.epam.training.microservicefoundation.resourceservice.service.kafka;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.epam.training.microservicefoundation.resourceservice.configuration.KafkaConfiguration;
import com.epam.training.microservicefoundation.resourceservice.configuration.KafkaTopicConfiguration;
import com.epam.training.microservicefoundation.resourceservice.model.ResourceRecord;
import com.epam.training.microservicefoundation.resourceservice.service.implementation.KafkaManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.SenderResult;
import reactor.test.StepVerifier;

@ExtendWith(value = {SpringExtension.class, KafkaExtension.class})
@ContextConfiguration(classes = {KafkaConfiguration.class, KafkaTopicConfiguration.class})
@TestPropertySource(locations = "classpath:application.properties")
class KafkaManagerTest {

    @Autowired
    private KafkaManager kafkaManager;

    @Test
    void shouldPublishMessage() {
      ResourceRecord message = new ResourceRecord(1L);
      Mono<SenderResult<Void>> senderResultMono = kafkaManager.publish(message);

      StepVerifier.create(senderResultMono)
          .assertNext(result -> {
            assertNull(result.exception());
            assertNotNull(result.recordMetadata());
            assertTrue(result.recordMetadata().hasOffset());
          }).verifyComplete();
    }

    @Test
    void shouldNotPublishNonexistentMessageType() {
        StepVerifier.create(kafkaManager.publish(new Object()))
            .expectError(IllegalStateException.class)
            .verify();
    }
}
