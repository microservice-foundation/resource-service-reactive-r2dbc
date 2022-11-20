package com.epam.training.microservicefoundation.resourceservice.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class AwsS3Configuration {

    @Value("${aws.s3.endpoint}")
    private String amazonS3Endpoint;
    @Value("${aws.s3.bucket-name}")
    private String amazonS3BucketName;
    @Value("${aws.s3.profile}")
    private String amazonS3Profile;
    @Value("${aws.s3.max.retry}")
    private int maxRetry;

    @Bean
    public S3Client getS3Client() {
        return S3Client.builder()
                .overrideConfiguration(clientOverrideConfiguration())
                .credentialsProvider(getProfileCredentialsProvider())
                .endpointOverride(URI.create(amazonS3Endpoint))
                .region(Region.US_EAST_1)
                .build();
    }

    private ClientOverrideConfiguration clientOverrideConfiguration() {
        return ClientOverrideConfiguration.builder()
                .retryPolicy(retryPolicy())
                .build();
    }

    private RetryPolicy retryPolicy() {
        return RetryPolicy.builder()
                .numRetries(maxRetry)
                .build();
    }

    private ProfileCredentialsProvider getProfileCredentialsProvider() {
        return ProfileCredentialsProvider.create(amazonS3Profile);
    }

    public String getAmazonS3Endpoint() {
        return amazonS3Endpoint;
    }

    public String getAmazonS3BucketName() {
        return amazonS3BucketName;
    }
}
