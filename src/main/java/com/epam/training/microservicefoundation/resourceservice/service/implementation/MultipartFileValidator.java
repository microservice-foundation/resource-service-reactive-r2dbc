package com.epam.training.microservicefoundation.resourceservice.service.implementation;

import com.epam.training.microservicefoundation.resourceservice.domain.ResourceType;
import com.epam.training.microservicefoundation.resourceservice.service.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.function.Predicate;

@Component
public class MultipartFileValidator implements Validator<MultipartFile> {
    private static final Logger log = LoggerFactory.getLogger(MultipartFileValidator.class);
    @Override
    public boolean validate(MultipartFile file) {
        log.info("Validating a multipart song-resource file");
        if(file == null || file.isEmpty()) {
            return false;
        }
        return file.getContentType() != null &&
                Arrays.stream(ResourceType.values())
                .anyMatch(type -> type.getMimeType().equals(file.getContentType()));
    }
}
