package com.epam.training.microservicefoundation.resourceservice.api;

import com.epam.training.microservicefoundation.resourceservice.model.APIError;
import com.epam.training.microservicefoundation.resourceservice.model.ResourceNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ResourceExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<APIError> handleIllegalArgumentException(IllegalArgumentException ex) {
        return buildResponseEntity(new APIError(HttpStatus.BAD_REQUEST, "Invalid request", ex));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<APIError> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return buildResponseEntity(new APIError(HttpStatus.NOT_FOUND, "Resource not found", ex));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<APIError> handleBusinessException(IllegalStateException ex) {
        return buildResponseEntity(new APIError(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error happened", ex));
    }

    private <T extends APIError> ResponseEntity<T> buildResponseEntity(T error) {
        return new ResponseEntity<>(error, error.getStatus());
    }
}
