package com.epam.training.microservicefoundation.resourceservice.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = WebClientProperties.PREFIX)
public class WebClientProperties {
  public static final String PREFIX = "web-client";
  private int connectionTimeout;
  private int responseTimeout;
  private int readTimeout;
  private int writeTimeout;
  private String baseUrl;

  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  public void setConnectionTimeout(int connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  public int getResponseTimeout() {
    return responseTimeout;
  }

  public void setResponseTimeout(int responseTimeout) {
    this.responseTimeout = responseTimeout;
  }

  public int getReadTimeout() {
    return readTimeout;
  }

  public void setReadTimeout(int readTimeout) {
    this.readTimeout = readTimeout;
  }

  public int getWriteTimeout() {
    return writeTimeout;
  }

  public void setWriteTimeout(int writeTimeout) {
    this.writeTimeout = writeTimeout;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }
}
