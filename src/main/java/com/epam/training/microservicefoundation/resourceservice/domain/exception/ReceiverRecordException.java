package com.epam.training.microservicefoundation.resourceservice.domain.exception;

import reactor.kafka.receiver.ReceiverRecord;

public class ReceiverRecordException extends RuntimeException {
  private ReceiverRecord<?, ?> record;
  public <T> ReceiverRecordException(ReceiverRecord<String, T> receiverRecord, Throwable error) {
    super(error);
    this.record = receiverRecord;
  }

  public ReceiverRecord<?, ?> getRecord() {
    return record;
  }
}
