package com.epam.training.microservicefoundation.resourceservice.model.exception;

import software.amazon.awssdk.core.SdkResponse;

public class UploadFailedException extends BaseBucketException {
  public UploadFailedException(SdkResponse response) {
    super(response);
  }
}
