package com.epam.training.microservicefoundation.resourceservice.service.convertor;

import com.epam.training.microservicefoundation.resourceservice.service.implementation.CsvFileConvertor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvFileConvertorTest {
    private CsvFileConvertor convertor;

    @BeforeEach
    void setup() {
        convertor = new CsvFileConvertor();
    }

    @Test
    void shouldConvert() throws IOException {
        File csvFile = ResourceUtils.getFile("classpath:files/ids.csv");

        MockMultipartFile multipartFile = new MockMultipartFile(csvFile.getName(), csvFile.getName(),
                "text/csv", Files.readAllBytes(csvFile.toPath()));

        List<Long> ids = convertor.covert(multipartFile);
        assertNotNull(ids);
        assertEquals(4, ids.size());
    }

    @Test
    void shouldThrowExceptionWhenConvertWithInvalidArguments() throws IOException {
        File csvFile = ResourceUtils.getFile("classpath:files/invalid_ids.csv");

        MockMultipartFile multipartFile = new MockMultipartFile(csvFile.getName(), csvFile.getName(),
                "text/csv", Files.readAllBytes(csvFile.toPath()));

        assertThrows(IllegalArgumentException.class, () -> convertor.covert(multipartFile));
    }
}