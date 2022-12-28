package com.epam.training.microservicefoundation.resourceservice.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class AwsS3Configuration {

    @Value("${aws.s3.endpoint}")
    private String amazonS3Endpoint;
    @Value("${aws.s3.credentials.access-key}")
    private String amazonS3AccessKey;
    @Value("${aws.s3.credentials.secret-key}")
    private String amazonS3SecretKey;
    @Value("${aws.s3.bucket-name}")
    private String amazonS3BucketName;

    @Bean
    public S3Client getS3Client() {
        return S3Client.builder()
                .credentialsProvider(getStaticCredentialsProvider())
                .endpointOverride(URI.create(amazonS3Endpoint))
                .region(Region.US_EAST_1)
                .build();
    }

    private StaticCredentialsProvider getStaticCredentialsProvider() {
        return StaticCredentialsProvider.create(getAwsBasicCredentials());
    }


    private AwsBasicCredentials getAwsBasicCredentials() {
        return AwsBasicCredentials.create(amazonS3AccessKey,
                amazonS3SecretKey);
    }

    public String getAmazonS3BucketName() {
        return amazonS3BucketName;
    }

    public String getAmazonS3Endpoint() {
        return amazonS3Endpoint;
    }
}
