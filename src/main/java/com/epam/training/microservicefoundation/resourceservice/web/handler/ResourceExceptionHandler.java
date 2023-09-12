package com.epam.training.microservicefoundation.resourceservice.web.handler;

import static jakarta.servlet.RequestDispatcher.ERROR_REQUEST_URI;
import static jakarta.servlet.RequestDispatcher.ERROR_STATUS_CODE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.epam.training.microservicefoundation.resourceservice.domain.exception.CloudStorageException;
import com.epam.training.microservicefoundation.resourceservice.domain.exception.EntityExistsException;
import com.epam.training.microservicefoundation.resourceservice.domain.exception.EntityNotFoundException;
import com.google.common.base.Throwables;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

public class ResourceExceptionHandler extends AbstractErrorWebExceptionHandler {
  private final Map<Class<? extends Throwable>, BiFunction<Throwable, ServerRequest, Mono<ServerResponse>>> exceptionServerResponse;
  private final ErrorAttributeOptions errorAttributeOptions;

  public ResourceExceptionHandler(ErrorAttributes errorAttributes, WebProperties.Resources resources,
      ApplicationContext applicationContext, ErrorAttributeOptions errorAttributeOptions) {
    super(errorAttributes, resources, applicationContext);
    this.errorAttributeOptions = errorAttributeOptions;
    this.exceptionServerResponse = new HashMap<>();

    exceptionServerResponse.put(EntityNotFoundException.class, (exception, request) -> buildResponseAttributes(request, NOT_FOUND, exception));
    exceptionServerResponse.put(EntityExistsException.class, (exception, request) -> buildResponseAttributes(request, METHOD_NOT_ALLOWED, exception));
    exceptionServerResponse.put(ResponseStatusException.class, (exception, request) -> buildResponseAttributes(request, BAD_REQUEST, exception));
    exceptionServerResponse.put(MethodArgumentNotValidException.class, (exception, request) -> buildResponseAttributes(request, BAD_REQUEST,
        exception));
    exceptionServerResponse.put(HttpMessageNotReadableException.class, (exception, request) -> buildResponseAttributes(request, BAD_REQUEST,
        exception));
    exceptionServerResponse.put(IllegalArgumentException.class, (exception, request) -> buildResponseAttributes(request, BAD_REQUEST,
        exception));
    exceptionServerResponse.put(NoSuchKeyException.class, (exception, request) -> buildResponseAttributes(request, NOT_FOUND, exception));
    exceptionServerResponse.put(CloudStorageException.class, (exception, request) -> buildResponseAttributes(request,
        HttpStatus.valueOf(((CloudStorageException)exception).getStatusCode()), exception));
  }

  @Override
  protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
    return RouterFunctions.route(RequestPredicates.all(), request -> {
      final Throwable error = getError(request);
      final BiFunction<Throwable, ServerRequest, Mono<ServerResponse>> exceptionHandler =
          exceptionServerResponse.getOrDefault(error.getClass(), (exception, request1) ->
              buildResponseAttributes(request1, INTERNAL_SERVER_ERROR, new RuntimeException("Hmm.. there is an unknown issue occurred")));

      return exceptionHandler.apply(error, request);
    });
  }

  private Mono<ServerResponse> buildResponseAttributes(ServerRequest request, HttpStatusCode status, Throwable exception) {
    request.attributes().put(ERROR_STATUS_CODE, status.value());
    request.attributes().put(ERROR_REQUEST_URI, request.uri());

    ErrorAttributes errorAttributes = new DefaultErrorAttributes() {
      @Override
      public Throwable getError(ServerRequest webRequest) {
        return Throwables.getRootCause(exception);
      }
    };

    Map<String, Object> body = errorAttributes.getErrorAttributes(request, errorAttributeOptions);
    fixBindingErrors(exception, body);
    return ServerResponse
        .status(status)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(body));
  }

  private void fixBindingErrors(Throwable exception, Map<String, Object> body) {
    if (errorAttributeOptions.isIncluded(ErrorAttributeOptions.Include.BINDING_ERRORS)
        && exception instanceof BindException bindException) {
      List<String> errors = bindException.getAllErrors().stream()
          .map(item -> ((FieldError) item).getField() + ": " + item.getDefaultMessage())
          .toList();
      body.put("errors", errors);
    }
  }
}
