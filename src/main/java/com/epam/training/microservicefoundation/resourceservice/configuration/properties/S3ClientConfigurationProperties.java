package com.epam.training.microservicefoundation.resourceservice.configuration.properties;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import software.amazon.awssdk.regions.Region;

@ConfigurationProperties(prefix = S3ClientConfigurationProperties.PREFIX)
public class S3ClientConfigurationProperties {
  public static final String PREFIX = "aws.s3";
  private Region region = Region.US_EAST_1;
  private URI endpoint;
  private int maxRetry;

  /**
   * AWS S3 requires that file parts must have at least 5MB, except for the last part. This may change for other S3-compatible services,
   * so let't define a configuration property for that.
   */
  private final int multipartMinPartSize = 5 * 1024 * 1024;

  public Region getRegion() {
    return region;
  }

  public void setRegion(Region region) {
    this.region = region;
  }

  public URI getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(URI endpoint) {
    this.endpoint = endpoint;
  }

  public int getMaxRetry() {
    return maxRetry;
  }

  public void setMaxRetry(int maxRetry) {
    this.maxRetry = maxRetry;
  }

  public int getMultipartMinPartSize() {
    return multipartMinPartSize;
  }
}
