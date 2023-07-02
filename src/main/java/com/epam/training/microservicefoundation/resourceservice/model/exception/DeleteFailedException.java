package com.epam.training.microservicefoundation.resourceservice.model.exception;

import software.amazon.awssdk.core.SdkResponse;

public class DeleteFailedException extends BaseBucketException {
  public DeleteFailedException(SdkResponse response) {
    super(response);
  }
}
