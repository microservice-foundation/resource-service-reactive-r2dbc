package com.epam.training.microservicefoundation.resourceservice.service.implementation;

import com.epam.training.microservicefoundation.resourceservice.service.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
@Component
public class IdParamValidator implements Validator<long[]> {
    private static final Logger log = LoggerFactory.getLogger(IdParamValidator.class);

    @Override
    public boolean validate(long[] ids) {
        log.info("Validating a multipart csv file");

        return ids != null && ids.length < 200;
    }
}
