package com.epam.training.microservicefoundation.resourceservice.model.exception;

import software.amazon.awssdk.core.SdkResponse;

public class CopyObjectFailedException extends BaseBucketException {
  public CopyObjectFailedException(SdkResponse response) {
    super(response);
  }
}
