package com.epam.training.microservicefoundation.resourceservice.service;


import com.epam.training.microservicefoundation.resourceservice.domain.Resource;
import com.epam.training.microservicefoundation.resourceservice.domain.ResourceMapper;
import com.epam.training.microservicefoundation.resourceservice.domain.ResourceNotFoundException;
import com.epam.training.microservicefoundation.resourceservice.domain.ResourceRecord;
import com.epam.training.microservicefoundation.resourceservice.repository.CloudStorageRepository;
import com.epam.training.microservicefoundation.resourceservice.repository.ResourceRepository;
import com.epam.training.microservicefoundation.resourceservice.service.implementation.IdParamValidator;
import com.epam.training.microservicefoundation.resourceservice.service.implementation.KafkaManager;
import com.epam.training.microservicefoundation.resourceservice.service.implementation.MultipartFileValidator;
import com.epam.training.microservicefoundation.resourceservice.service.implementation.ResourceServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.ResourceUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;
    @Mock
    private CloudStorageRepository storageRepository;
    @Mock
    private ResourceMapper mapper;
    @Mock
    private MultipartFileValidator validator;
    @Mock
    private IdParamValidator idParamValidator;
    @Mock
    private KafkaManager kafkaManager;
    private ResourceService service;

    @BeforeEach
    public void setup() {
        service = new ResourceServiceImpl(resourceRepository, storageRepository, mapper, validator, idParamValidator,
                kafkaManager);
    }

    @Test
    void shouldSaveSong() throws IOException {

        File song = ResourceUtils.getFile("classpath:files/mpthreetest.mp3");
        String path = "https:localhost:8080/s3/resource-service/mpthreetest.mp3";

        long id = 1L;
        when(storageRepository.upload(any())).thenReturn(path);
        when(resourceRepository.save(any())).thenReturn(
                new Resource.Builder(path, song.getName()).id(id).build());

        when(validator.validate(any())).thenReturn(Boolean.TRUE);
        when(mapper.mapToRecord(any())).thenReturn(new ResourceRecord(id));

        MockMultipartFile multipartFile = new MockMultipartFile(song.getName(), Files.readAllBytes(song.toPath()));
        ResourceRecord record = service.save(multipartFile);

        Assertions.assertNotNull(record);
        Assertions.assertEquals(id, record.getId());

        verify(storageRepository, only()).upload(any());
        verify(resourceRepository, only()).save(any());
        verify(validator, only()).validate(any());
        verify(mapper, only()).mapToRecord(any());
    }

    @Test
    void shouldThrowExceptionWhenSaveInvalidMultipartFile() throws IOException {
        File song = ResourceUtils.getFile("classpath:files/mpthreetest.mp3");
        when(validator.validate(any())).thenReturn(Boolean.FALSE);

        MockMultipartFile multipartFile = new MockMultipartFile(song.getName(), Files.readAllBytes(song.toPath()));
        assertThrows(IllegalArgumentException.class, () -> service.save(multipartFile));

        verify(validator, only()).validate(multipartFile);
    }


    @Test
    void shouldGetSong() throws IOException {
        long id = 1L;
        File song = ResourceUtils.getFile("classpath:files/mpthreetest.mp3");
        when(resourceRepository.findById(id)).thenReturn(Optional.of(new Resource.Builder(song.toPath().toString(),
                song.getName()).build()));

        when(storageRepository.getByName(song.getName())).thenReturn(
                new ResponseInputStream<>(GetObjectResponse.builder().build(), AbortableInputStream.createEmpty()));

        InputStreamResource fileData = service.getById(id);
        assertNotNull(fileData);

        verify(resourceRepository, only()).findById(id);
        verify(storageRepository, only()).getByName(song.getName());
    }

    @Test
    void shouldThrowExceptionWhenGetNonexistentSong() {
        assertThrows(ResourceNotFoundException.class, () -> service.getById(1L));
    }

    @Test
    void shouldDeleteSong() throws IOException {
        when(idParamValidator.validate(any())).thenReturn(Boolean.TRUE);
        long id = 1L;
        String name = "test.mp3";
        when(resourceRepository.findById(id)).thenReturn(Optional.of(new Resource.Builder("test/test.mp3", name).build()));
        doNothing().when(storageRepository).deleteByName(any());
        service.deleteByIds(new long[]{1L});

        verify(idParamValidator, times(1)).validate(any());
        verify(resourceRepository, times(1)).findById(id);
        verify(storageRepository, times(1)).deleteByName(name);
    }

    @Test
    void shouldThrowExceptionWhenDeleteInvalidData() throws IOException {
        when(idParamValidator.validate(any())).thenReturn(Boolean.FALSE);
        assertThrows(IllegalArgumentException.class, () -> service.deleteByIds(new long[200]));
    }

}
