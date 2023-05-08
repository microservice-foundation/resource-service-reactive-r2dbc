package com.epam.training.microservicefoundation.resourceservice.service.kafka;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public class KafkaExtension implements BeforeAllCallback, AfterAllCallback {

    @Container
    private KafkaContainer container;

    @Override
    public void afterAll(ExtensionContext context) {
      container.stop();
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        container = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.4.3"));
        container.start();

        System.setProperty("spring.kafka.producer.bootstrap-servers", container.getBootstrapServers());
    }
}
