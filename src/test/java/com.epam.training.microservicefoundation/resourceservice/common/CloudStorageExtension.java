package com.epam.training.microservicefoundation.resourceservice.common;

import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;


@Testcontainers
public final class CloudStorageExtension implements BeforeAllCallback, AfterAllCallback {
  @Container
  private LocalStackContainer localStack;

  @Override
  public void afterAll(ExtensionContext context) {
    localStack.stop();
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    final Properties properties = loadApplicationProperties();
    localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:1.0.0")).withServices(S3);
    localStack.start();
    localStack.execInContainer("awslocal", "s3", "mb", "s3://" + properties.getProperty("aws.s3.staging-bucket"));
    localStack.execInContainer("awslocal", "s3", "mb", "s3://" + properties.getProperty("aws.s3.permanent-bucket"));

    System.setProperty("AWS_S3_URL", localStack.getEndpointOverride(S3).toString());
    System.setProperty("AWS_S3_ACCESS_KEY", localStack.getAccessKey());
    System.setProperty("AWS_S3_SECRET_KEY", localStack.getSecretKey());
    System.setProperty("AWS_S3_REGION", localStack.getRegion());
  }

  private Properties loadApplicationProperties() throws IOException {
    ClassPathResource resource = new ClassPathResource("application.properties");
    return PropertiesLoaderUtils.loadProperties(resource);
  }
}
