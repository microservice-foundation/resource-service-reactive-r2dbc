package com.epam.training.microservicefoundation.resourceservice.api;

import com.epam.training.microservicefoundation.resourceservice.model.APIError;
import com.epam.training.microservicefoundation.resourceservice.model.exception.DeleteFailedException;
import com.epam.training.microservicefoundation.resourceservice.model.exception.DownloadFailedException;
import com.epam.training.microservicefoundation.resourceservice.model.exception.ResourceNotFoundException;
import com.epam.training.microservicefoundation.resourceservice.model.exception.UploadFailedException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

public class ResourceExceptionHandler extends AbstractErrorWebExceptionHandler {
  private final Map<Class<? extends Throwable>, Function<Throwable, Mono<ServerResponse>>> exceptionToHandlers;

  public ResourceExceptionHandler(ErrorAttributes errorAttributes, WebProperties.Resources resources,
      ApplicationContext applicationContext) {
    super(errorAttributes, resources, applicationContext);
    exceptionToHandlers = new HashMap<>();
    Function<Throwable, Mono<ServerResponse>> invalidRequest = throwable -> ServerResponse
        .status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(new APIError(HttpStatus.BAD_REQUEST, "Invalid request", throwable)));

    exceptionToHandlers.put(IllegalArgumentException.class, invalidRequest);
    exceptionToHandlers.put(NumberFormatException.class, invalidRequest);
    exceptionToHandlers.put(DataIntegrityViolationException.class, invalidRequest);

    exceptionToHandlers.put(ResourceNotFoundException.class, throwable -> ServerResponse
        .status(HttpStatus.NOT_FOUND)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(new APIError(HttpStatus.NOT_FOUND, "Resource is not found", throwable))));

    exceptionToHandlers.put(IllegalStateException.class, throwable -> ServerResponse
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(new APIError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error has happened",
            throwable))));

    exceptionToHandlers.put(DeleteFailedException.class, throwable -> {
      DeleteFailedException deleteFailedException = (DeleteFailedException) throwable;
      return ServerResponse
          .status(HttpStatus.valueOf(deleteFailedException.getStatusCode()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(new APIError(HttpStatus.valueOf(deleteFailedException.getStatusCode()),
              "Resource file deletion has failed: " + deleteFailedException.getStatusText(), throwable)));
    });

    exceptionToHandlers.put(DownloadFailedException.class, throwable -> {
      DownloadFailedException downloadFailedException = (DownloadFailedException) throwable;
      return ServerResponse
          .status(HttpStatus.valueOf(downloadFailedException.getStatusCode())).contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(new APIError(HttpStatus.valueOf(downloadFailedException.getStatusCode()),
              "Resource file download has failed: " + downloadFailedException.getStatusText(), throwable)));
    });

    exceptionToHandlers.put(UploadFailedException.class, throwable -> {
      UploadFailedException uploadFailedException = (UploadFailedException) throwable;
      return ServerResponse
          .status(HttpStatus.valueOf(uploadFailedException.getStatusCode())).contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(new APIError(HttpStatus.valueOf(uploadFailedException.getStatusCode()),
              "Resource file upload has failed: " + uploadFailedException.getStatusText(), throwable)));
    });

    exceptionToHandlers.put(NoSuchKeyException.class, throwable -> ServerResponse
        .status(HttpStatus.NOT_FOUND)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(new APIError(HttpStatus.NOT_FOUND, "Resource file is not found", throwable))));
  }

  @Override
  protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
    return RouterFunctions.route(RequestPredicates.all(), request -> {
      Throwable error = getError(request);
      if (exceptionToHandlers.containsKey(error.getClass())) {
        return exceptionToHandlers.get(error.getClass()).apply(error);
      }
      return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(BodyInserters.fromValue(new APIError(HttpStatus.INTERNAL_SERVER_ERROR,
              "Hmm.. there is an unknown issue occurred", error)));
    });
  }
}
