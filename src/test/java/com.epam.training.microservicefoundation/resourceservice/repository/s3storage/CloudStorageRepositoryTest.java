package com.epam.training.microservicefoundation.resourceservice.repository.s3storage;

import com.epam.training.microservicefoundation.resourceservice.configuration.AwsS3Configuration;
import com.epam.training.microservicefoundation.resourceservice.domain.ResourceNotFoundException;
import com.epam.training.microservicefoundation.resourceservice.repository.CloudStorageRepository;
import com.epam.training.microservicefoundation.resourceservice.repository.TestStorageContext;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ExtendWith(CloudStorageExtension.class)
@ContextConfiguration(classes = {AwsS3Configuration.class, TestStorageContext.class})
class CloudStorageRepositoryTest {

    @Autowired
    private CloudStorageRepository repository;

    @Test
    void shouldUploadSong() throws IOException {
        File song = ResourceUtils.getFile("classpath:files/mpthreetest.mp3");
        MultipartFile file = new MockMultipartFile(song.getName(), new FileInputStream(song));
        String path = repository.upload(file);
        assertNotNull(path);
    }

    @Test
    void shouldGetSong() throws IOException {
        // upload a file
        File song = ResourceUtils.getFile("classpath:files/mpthreetest.mp3");
        MultipartFile file = new MockMultipartFile(song.getName(), new FileInputStream(song));
        String path = repository.upload(file);
        assertNotNull(path);

        ResponseInputStream<GetObjectResponse> inputStream = repository.getByName(song.getName());
        assertNotNull(inputStream);
        assertTrue(inputStream.response().contentLength() > 0);
    }

    @Test
    void shouldThrowExceptionWhenGetSongWithNonexistentName() {
        assertThrows(ResourceNotFoundException.class, () -> repository.getByName("nonexistent.mp3"));
    }

    @Test
    void shouldDeleteSong() throws IOException {
        // upload a song
        File song = ResourceUtils.getFile("classpath:files/mpthreetest.mp3");
        MultipartFile file = new MockMultipartFile(song.getName(), new FileInputStream(song));
        String path = repository.upload(file);
        assertNotNull(path);

        String fileName = song.getName();
        repository.deleteByName(fileName);

        // try to get the deleted object
        assertThrows(ResourceNotFoundException.class, () -> repository.getByName(fileName));
    }
}
