package com.epam.training.microservicefoundation.resourceservice.repository;

import com.epam.training.microservicefoundation.resourceservice.configuration.AwsS3Configuration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.s3.S3Client;

@TestConfiguration
public class TestStorageContext {

    @Bean
    CloudStorageRepository cloudStorageRepository(AwsS3Configuration configuration, S3Client s3Client) {
        return new CloudStorageRepository(configuration, s3Client);
    }
}
