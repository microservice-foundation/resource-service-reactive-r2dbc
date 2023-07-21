package com.epam.training.microservicefoundation.resourceservice.common;

import org.apache.kafka.clients.producer.RecordMetadata;
import reactor.kafka.sender.SenderResult;

public class FakeSenderResult<T> implements SenderResult<T> {
    private final RecordMetadata recordMetadata;
    private final Exception exception;
    private final T correlationMetadata;

    public FakeSenderResult(RecordMetadata recordMetadata, Exception exception, T correlationMetadata) {
      this.recordMetadata = recordMetadata;
      this.exception = exception;
      this.correlationMetadata = correlationMetadata;
    }

    @Override
    public RecordMetadata recordMetadata() {
      return this.recordMetadata;
    }

    @Override
    public Exception exception() {
      return this.exception;
    }

    @Override
    public T correlationMetadata() {
      return this.correlationMetadata;
    }
  }