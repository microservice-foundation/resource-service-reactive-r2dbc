package com.epam.training.microservicefoundation.resourceservice.configuration;

import com.epam.training.microservicefoundation.resourceservice.repository.CloudStorageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@TestConfiguration
public class AwsS3TestConfiguration {

    @Value("${aws.s3.endpoint}")
    private String amazonS3Endpoint;
    @Value("${aws.s3.credentials.access-key}")
    private String amazonS3AccessKey;
    @Value("${aws.s3.credentials.secret-key}")
    private String amazonS3SecretKey;
    @Value("${aws.s3.bucket-name}")
    private String amazonS3BucketName;

    @Bean
    public CloudStorageRepository cloudStorageRepository() {
        return new CloudStorageRepository(amazonS3BucketName, amazonS3Endpoint, s3Client());
    }

    private S3Client s3Client() {
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
}
