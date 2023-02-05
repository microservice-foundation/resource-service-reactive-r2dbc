package com.epam.training.microservicefoundation.resourceservice.repository;

import com.epam.training.microservicefoundation.resourceservice.model.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;


public class CloudStorageRepository {

    private static final Logger log = LoggerFactory.getLogger(CloudStorageRepository.class);
    private final String bucketName;
    private final String s3Endpoint;
    private final S3Client s3Client;

    @Autowired
    public CloudStorageRepository(String bucketName, String s3Endpoint, S3Client s3Client) {
        this.bucketName = bucketName;
        this.s3Endpoint = s3Endpoint;
        this.s3Client = s3Client;

    }

    public String upload(MultipartFile file) {
        log.info("Uploading file '{}' to bucket '{}'", file.getName(), bucketName);

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
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
                    bucketName, putObjectResponse);

            return s3Endpoint + "/" + bucketName + "/" + file.getOriginalFilename();
        }

        IllegalStateException ex = new IllegalStateException(String.format("File '%1s' upload failed to bucket '%2s'",
                file.getOriginalFilename(), bucketName));

        log.error("Uploading file failed:", ex);
        throw ex;
    }

    public ResponseInputStream<GetObjectResponse> getByName(String name) {
        log.info("Getting song by name '{}' from bucket '{}'", name, bucketName);
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
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
        log.info("Deleting a song '{}' from bucket '{}'", name, bucketName);

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(name)
                .build();

        s3Client.deleteObject(request);
    }

}
