package com.epam.training.microservicefoundation.resourceservice.router;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.web.reactive.function.server.RequestPredicates.DELETE;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RequestPredicates.contentType;

import com.epam.training.microservicefoundation.resourceservice.handler.ResourceHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class ResourceRouter {
  private static final String QUERY_PARAM_ID = "id";

  @Bean
  RouterFunction<ServerResponse> routes(ResourceHandler handler) {
    return RouterFunctions.nest(RequestPredicates.path("/api/v1/resources"),
        RouterFunctions
            .route(GET("/{id}").and(accept(APPLICATION_OCTET_STREAM)), handler::getById)
            .andRoute(POST("").and(accept(APPLICATION_JSON)).and(contentType(MULTIPART_FORM_DATA)), handler::save)
            .andRoute(DELETE("").and(RequestPredicates.queryParam(QUERY_PARAM_ID, t -> true)).and(accept(APPLICATION_JSON)),
                request -> handler.deleteByIds(request, QUERY_PARAM_ID)));
  }
}
