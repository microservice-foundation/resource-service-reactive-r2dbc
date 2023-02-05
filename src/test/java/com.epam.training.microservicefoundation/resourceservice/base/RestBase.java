package com.epam.training.microservicefoundation.resourceservice.base;

import com.epam.training.microservicefoundation.resourceservice.api.ResourceController;
import com.epam.training.microservicefoundation.resourceservice.api.ResourceExceptionHandler;
import com.epam.training.microservicefoundation.resourceservice.model.ResourceNotFoundException;
import com.epam.training.microservicefoundation.resourceservice.model.ResourceRecord;
import com.epam.training.microservicefoundation.resourceservice.service.ResourceService;
import com.epam.training.microservicefoundation.resourceservice.service.Validator;
import com.epam.training.microservicefoundation.resourceservice.service.implementation.MultipartFileValidator;
import io.restassured.config.EncoderConfig;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.config.RestAssuredMockMvcConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Objects;

import static com.epam.training.microservicefoundation.resourceservice.model.ResourceType.MP3;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT) // https://stackoverflow.com/questions/42947613/how-to-resolve-unneccessary-stubbing-exception
@ContextConfiguration(classes = RestBase.RestBaseConfiguration.class)
public abstract class RestBase {
    private ResourceController resourceController;
    @Autowired
    ResourceExceptionHandler resourceExceptionHandler;
    @MockBean
    ResourceService service;
    private Validator<MultipartFile> multipartFileValidator;

    @BeforeEach
    public void setup() throws FileNotFoundException {
        multipartFileValidator = new MultipartFileValidator();
        when(service.getById(123L)).thenReturn(new InputStreamResource(new FileInputStream(ResourceUtils.getFile(
                "classpath:files/mpthreetest.mp3"))));

        when(service.getById(1999L)).thenThrow(new ResourceNotFoundException("Resource with id=1999 not found"));
//        when(service.save(argThat(new SuccessfulMultipartMatcher()))).thenReturn(new ResourceRecord(1L));
        when(service.save(argThat(argument -> TRUE.equals(multipartFileValidator.validate(argument)))))
                .thenReturn(new ResourceRecord(1L));

        when(service.save(argThat(argument -> FALSE.equals(multipartFileValidator.validate(argument))))).thenThrow(
                new IllegalArgumentException("File with name 'bad-request.mp4' was not validated, check your file"));

        when(service.deleteByIds(new long[]{1L})).thenReturn(Collections.singletonList(new ResourceRecord(1L)));
        when(service.deleteByIds(argThat(argument -> argument == null || argument.length == 0 || argument.length > 200)))
                .thenThrow(new IllegalArgumentException("Id param was not validated, check your file"));

        resourceController = new ResourceController(service);
        EncoderConfig encoderConfig = new EncoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false);
        RestAssuredMockMvc.config = new RestAssuredMockMvcConfig().encoderConfig(encoderConfig);
        RestAssuredMockMvc.standaloneSetup(MockMvcBuilders.standaloneSetup(resourceController)
                .setControllerAdvice(resourceExceptionHandler));
    }

    @TestConfiguration
    static class RestBaseConfiguration {

        @Bean
        ResourceExceptionHandler resourceExceptionHandler() {
            return new ResourceExceptionHandler();
        }
    }

    static class SuccessfulMultipartMatcher implements ArgumentMatcher<MultipartFile> {
        @Override
        public boolean matches(MultipartFile argument) {
            return !argument.isEmpty() && MP3.getMimeType().equals(argument.getContentType()) &&
                    Objects.requireNonNull(argument.getOriginalFilename()).endsWith(MP3.getExtension());
        }
    }
}
