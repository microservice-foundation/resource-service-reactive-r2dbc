package com.epam.training.microservicefoundation.resourceservice.repository;

import com.epam.training.microservicefoundation.resourceservice.configuration.AwsS3Configuration;
import com.epam.training.microservicefoundation.resourceservice.domain.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

@Repository
public class CloudStorageRepository {

    private static final Logger log = LoggerFactory.getLogger(CloudStorageRepository.class);
    private final AwsS3Configuration configuration;
    private final S3Client s3Client;

    @Autowired
    public CloudStorageRepository(AwsS3Configuration configuration, S3Client s3Client) {
        this.configuration = configuration;
        this.s3Client = s3Client;
    }

    public String upload(MultipartFile file) {
        log.info("Uploading file '{}' to bucket '{}'", file.getName(), configuration);

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(configuration.getAmazonS3BucketName())
                .key(file.getOriginalFilename())
                .build();

        PutObjectResponse putObjectResponse;
        try(InputStream inputStream = file.getInputStream()) {
             putObjectResponse = s3Client.putObject(objectRequest,
                    RequestBody.fromInputStream(inputStream, file.getSize()));

        } catch (IOException ex) {
            IllegalStateException illegalStateException = new IllegalStateException(ex);
            log.error("Getting input stream failed from multipart file '{}'", file, illegalStateException);
            throw illegalStateException;
        }

        if(Objects.nonNull(putObjectResponse)) {
            log.debug("File '{}' uploaded successfully to bucket '{}': response: {}", file.getOriginalFilename(),
                    configuration.getAmazonS3BucketName(), putObjectResponse);

            return configuration.getAmazonS3Endpoint() + "/" + configuration.getAmazonS3BucketName() + "/" + file.getOriginalFilename();
        }

        IllegalStateException ex = new IllegalStateException(String.format("File '%1s' upload failed to bucket '%2s'",
                file.getOriginalFilename(), configuration.getAmazonS3BucketName()));

        log.error("Uploading file failed:", ex);
        throw ex;
    }

    public ResponseInputStream<GetObjectResponse> getByName(String name) {
        log.info("Getting song by name '{}' from bucket '{}'", name, configuration.getAmazonS3BucketName());
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(configuration.getAmazonS3BucketName())
                .key(name)
                .build();
        try {
            return s3Client.getObject(request);
        } catch (NoSuchKeyException ex) {
            ResourceNotFoundException exception = new ResourceNotFoundException(String.format("Resource was not found with " +
                    "key %1s", name), ex);

            log.error("Resource not found with key '{}' \nreason: ", name, ex);
            throw exception;
        }
    }

    public void deleteByName(String name) {
        log.info("Deleting a song '{}' from bucket '{}'", name, configuration.getAmazonS3BucketName());

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(configuration.getAmazonS3BucketName())
                .key(name)
                .build();

        s3Client.deleteObject(request);
    }

}
