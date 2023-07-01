package com.epam.training.microservicefoundation.resourceservice.model.exception;

import software.amazon.awssdk.core.SdkResponse;

public class DownloadFailedException extends BaseBucketException {
  public DownloadFailedException(SdkResponse response) {
    super(response);
  }
}
