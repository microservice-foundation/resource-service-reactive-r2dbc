package com.epam.training.microservicefoundation.resourceservice.service.validator;

import com.epam.training.microservicefoundation.resourceservice.service.implementation.MultipartFileValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultipartFileValidatorTest {
    private MultipartFileValidator validator;
    @BeforeEach
    void setup() {
        validator = new MultipartFileValidator();
    }

    @Test
    void shouldBeValid() throws IOException {
        File songFile = ResourceUtils.getFile("classpath:files/mpthreetest.mp3");
        MockMultipartFile multipartFile = new MockMultipartFile(
                songFile.getName(), songFile.getName(), "audio/mpeg", Files.readAllBytes(songFile.toPath()));

        boolean isValid = validator.validate(multipartFile);
        assertTrue(isValid);
    }

    @Test
    void shouldBeInvalidWithEmptyContent() {
        MockMultipartFile multipartFile =
                new MockMultipartFile("test.mp3", "test.mp3", "audio/mpeg", new byte[0]);

        boolean isValid = validator.validate(multipartFile);
        assertFalse(isValid);
    }

    @Test
    void shouldBeInvalidWithDifferentContentType() {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "test.mp3", "text.mp3", "text/csv", new byte[]{1,3,4,6,7});

        boolean isValid = validator.validate(multipartFile);
        assertFalse(isValid);
    }
}