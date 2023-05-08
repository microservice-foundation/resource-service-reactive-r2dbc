package com.epam.training.microservicefoundation.resourceservice.model.exception;

import org.springframework.http.HttpStatus;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.http.SdkHttpResponse;

public class UploadFailedException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private final int statusCode;
  private final String statusText;

  public UploadFailedException(SdkResponse response) {

    SdkHttpResponse httpResponse = response.sdkHttpResponse();
    if (httpResponse != null) {
      this.statusCode = httpResponse.statusCode();
      statusText = httpResponse.statusText().orElse("UNKNOWN");
    } else {
      this.statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
      statusText = "UNKNOWN";
    }
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getStatusText() {
    return statusText;
  }
}
