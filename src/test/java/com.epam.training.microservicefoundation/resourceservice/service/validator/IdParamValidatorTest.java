package com.epam.training.microservicefoundation.resourceservice.service.validator;

import com.epam.training.microservicefoundation.resourceservice.service.implementation.IdParamValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class IdParamValidatorTest {
    private IdParamValidator validator;

    @BeforeEach
    void setup() {
        validator = new IdParamValidator();
    }

    @Test
    void shouldBeValid() throws IOException {

        boolean isValid = validator.validate(new long[] {1L, 234L, 123_543_532L, 99L, 7776L});
        assertTrue(isValid);
    }

    @Test
    void shouldBeInvalidWithNull() {
        boolean isValid = validator.validate(null);
        assertFalse(isValid);
    }

    @Test
    void shouldBeInvalidWithEmptyContent() {
        boolean isValid = validator.validate(new long[200]);
        assertFalse(isValid);
    }

}