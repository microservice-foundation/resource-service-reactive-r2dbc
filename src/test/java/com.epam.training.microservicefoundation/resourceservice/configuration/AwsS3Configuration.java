package com.epam.training.microservicefoundation.resourceservice.configuration;

import com.epam.training.microservicefoundation.resourceservice.configuration.properties.S3ClientConfigurationProperties;
import com.epam.training.microservicefoundation.resourceservice.repository.CloudStorageRepository;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;

@TestConfiguration
@EnableConfigurationProperties(value = S3ClientConfigurationProperties.class)
public class AwsS3Configuration {

  @Value("${aws.s3.access-key}")
  private String accessKey;
  @Value("${aws.s3.secret-key}")
  private String secretKey;
  @Bean
  public CloudStorageRepository cloudStorageRepository(S3ClientConfigurationProperties properties) {
    return new CloudStorageRepository(properties, s3Client(properties));
  }

  private S3AsyncClient s3Client(S3ClientConfigurationProperties properties) {
    SdkAsyncHttpClient httpClient = NettyNioAsyncHttpClient.builder()
        .writeTimeout(Duration.ZERO)
        .maxConcurrency(64)
        .build();

    S3Configuration serviceConfiguration = S3Configuration.builder()
        .checksumValidationEnabled(false)
        .chunkedEncodingEnabled(true)
        .build();

    return S3AsyncClient.builder()
        .httpClient(httpClient)
        .credentialsProvider(getStaticCredentialsProvider())
        .region(properties.getRegion())
        .endpointOverride(properties.getEndpoint())
        .serviceConfiguration(serviceConfiguration)
        .overrideConfiguration(clientOverrideConfiguration(properties))
        .build();
  }

  private StaticCredentialsProvider getStaticCredentialsProvider() {
    return StaticCredentialsProvider.create(getAwsBasicCredentials());
  }

  private ClientOverrideConfiguration clientOverrideConfiguration(S3ClientConfigurationProperties properties) {
    return ClientOverrideConfiguration.builder()
        .retryPolicy(retryPolicy(properties))
        .build();
  }

  private RetryPolicy retryPolicy(S3ClientConfigurationProperties properties) {
    return RetryPolicy.builder()
        .retryCondition(RetryCondition.defaultRetryCondition())
        .backoffStrategy(BackoffStrategy.defaultStrategy())
        .numRetries(properties.getMaxRetry())
        .build();
  }

  private AwsBasicCredentials getAwsBasicCredentials() {
    return AwsBasicCredentials.create(accessKey, secretKey);
  }
}
