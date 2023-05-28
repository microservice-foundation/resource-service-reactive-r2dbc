package com.epam.training.microservicefoundation.resourceservice.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import software.amazon.awssdk.regions.Region;

@ConfigurationProperties(prefix = "aws.s3")
@ConstructorBinding
public class S3ClientConfigurationProperties {
  private Region region = Region.US_EAST_1;
  private URI endpoint;
  private String bucketName;
  private int maxRetry;

  // AWS S3 requires that file parts must have at least 5MB, except
  // for the last part. This may change for other S3-compatible services, so let't
  // define a configuration property for that
  private final int multipartMinPartSize = 5 * 1024 * 1024;

  public S3ClientConfigurationProperties(URI endpoint, String bucketName, int maxRetry) {
    this.endpoint = endpoint;
    this.bucketName = bucketName;
    this.maxRetry = maxRetry;
  }

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

  public String getBucketName() {
    return bucketName;
  }

  public void setBucketName(String bucketName) {
    this.bucketName = bucketName;
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
