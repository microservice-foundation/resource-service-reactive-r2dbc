package com.epam.training.microservicefoundation.resourceservice.domain.exception;

import org.springframework.http.HttpStatus;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.http.SdkHttpResponse;

public final class CloudStorageException extends RuntimeException {
  private final int statusCode;
  private final String statusText;

  public CloudStorageException(SdkResponse response) {

    SdkHttpResponse httpResponse = response.sdkHttpResponse();
    if (httpResponse != null) {
      this.statusCode = httpResponse.statusCode();
      this.statusText = httpResponse.statusText().orElse("UNKNOWN");
    } else {
      this.statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
      this.statusText = "UNKNOWN";
    }

  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getStatusText() {
    return statusText;
  }
}
