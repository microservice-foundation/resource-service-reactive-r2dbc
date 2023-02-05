package com.epam.training.microservicefoundation.resourceservice.service.kafka;

import com.epam.training.microservicefoundation.resourceservice.model.ResourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.concurrent.CountDownLatch;

public class FakeKafkaConsumer {
    private final Logger log = LoggerFactory.getLogger(FakeKafkaConsumer.class);
    private CountDownLatch latch = new CountDownLatch(1);
    private ResourceRecord resourceRecord;

    @KafkaListener(topics = "resources")
    public void listen(ResourceRecord resourceRecord) {
        log.info("A message received from the resources topic': {}", resourceRecord);
        this.resourceRecord = resourceRecord;
        latch.countDown();
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    public void resetLatch() {
        latch = new CountDownLatch(1);
    }

    public ResourceRecord resourceRecord() {
        return resourceRecord;
    }
}
