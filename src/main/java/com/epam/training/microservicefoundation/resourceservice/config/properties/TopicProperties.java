package com.epam.training.microservicefoundation.resourceservice.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = TopicProperties.PREFIX)
public class TopicProperties {
  public static final String PREFIX = "kafka.topic";
  private String resourceStaging;
  private String resourcePermanent;

  private final Properties properties = new Properties();

  public String getResourceStaging() {
    return resourceStaging;
  }

  public String getResourcePermanent() {
    return resourcePermanent;
  }

  public Properties getProperties() {
    return properties;
  }

  public void setResourceStaging(String resourceStaging) {
    this.resourceStaging = resourceStaging;
  }

  public void setResourcePermanent(String resourcePermanent) {
    this.resourcePermanent = resourcePermanent;
  }

  public static class Properties {
    private int partitionCount;
    private int replicationFactor;

    public int getPartitionCount() {
      return partitionCount;
    }

    public int getReplicationFactor() {
      return replicationFactor;
    }

    public void setPartitionCount(int partitionCount) {
      this.partitionCount = partitionCount;
    }

    public void setReplicationFactor(int replicationFactor) {
      this.replicationFactor = replicationFactor;
    }
  }
}
