package com.epam.training.microservicefoundation.resourceservice.web.handler;

import com.epam.training.microservicefoundation.resourceservice.domain.dto.DeleteResourceDTO;
import com.epam.training.microservicefoundation.resourceservice.domain.dto.GetResourceDTO;
import com.epam.training.microservicefoundation.resourceservice.service.BaseResourceService;
import com.epam.training.microservicefoundation.resourceservice.service.implementation.StageResourceService;
import com.epam.training.microservicefoundation.resourceservice.web.validator.RequestQueryParamValidator;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ResourceHandler {
  private static final Logger log = LoggerFactory.getLogger(ResourceHandler.class);
  private final BaseResourceService baseService;
  private final StageResourceService stageResourceService;
  private final RequestQueryParamValidator idQueryParamValidator;

  @Autowired
  public ResourceHandler(BaseResourceService baseService, StageResourceService stageResourceService,
      RequestQueryParamValidator idQueryParamValidator) {
    this.baseService = baseService;
    this.stageResourceService = stageResourceService;
    this.idQueryParamValidator = idQueryParamValidator;
  }

  public Mono<ServerResponse> save(final ServerRequest request) {
    log.info("Incoming request: {}", request);
    Mono<MultiValueMap<String, Part>> multiValueMapMono = request.body(BodyExtractors.toMultipartData());
    Mono<FilePart> filePart = multiValueMapMono.flatMap(map -> Mono.justOrEmpty(map.getFirst("file"))).ofType(FilePart.class);

    return ServerResponse.created(URI.create(request.path()))
        .contentType(MediaType.APPLICATION_JSON)
        .body(stageResourceService.saveToStage(filePart), GetResourceDTO.class);
  }

  public Mono<ServerResponse> deleteByIds(final ServerRequest request, final String queryParam) {
    log.info("Incoming request: {}", request);
    final String validQueryParamValue = idQueryParamValidator.validateQueryParam(request.queryParam(queryParam), queryParam);

    return ServerResponse.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(baseService.deleteByIds(getIds(validQueryParamValue)), DeleteResourceDTO.class);
  }

  public Mono<ServerResponse> getById(final ServerRequest request) {
    log.info("Incoming request: {}", request);
    final long id = Long.parseLong(request.pathVariable("id"));
    return baseService.getById(id)
        .flatMap(responsePublisher -> ServerResponse.ok()
            .header(HttpHeaders.CONTENT_TYPE, responsePublisher.response().contentType())
            .header(HttpHeaders.CONTENT_LENGTH, Long.toString(responsePublisher.response().contentLength()))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + responsePublisher.response().metadata()
                    .getOrDefault("filename", "UNKNOWN") + "\"")
            .body(Flux.from(responsePublisher), ByteBuffer.class));
  }

  private Long[] getIds(final String paramValue) {
    return Arrays.stream(paramValue.split(","))
        .map(String::trim)
        .map(Long::valueOf)
        .toArray(Long[]::new);
  }
}
