package com.epam.training.microservicefoundation.resourceservice.repository.s3storage;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;


@Testcontainers
public final class CloudStorageExtension implements BeforeAllCallback, AfterAllCallback {

    @Container
    private LocalStackContainer localStack;

    private final static String STORAGE_BUCKET_NAME = "song-resource";

    @Override
    public void afterAll(ExtensionContext context) throws Exception {

    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:1.0.0"))
                .withServices(S3);

        localStack.start();
        localStack.execInContainer("awslocal", "s3", "mb", "s3://" + STORAGE_BUCKET_NAME);

        System.setProperty("aws.s3.endpoint", localStack.getEndpointOverride(S3).toString());
        System.setProperty("aws.s3.credentials.access-key", localStack.getAccessKey());
        System.setProperty("aws.s3.credentials.secret-key", localStack.getSecretKey());
        System.setProperty("aws.credentials.region", localStack.getRegion());
    }
}
