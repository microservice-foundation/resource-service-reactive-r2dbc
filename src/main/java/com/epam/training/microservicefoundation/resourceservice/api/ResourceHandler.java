package com.epam.training.microservicefoundation.resourceservice.api;

import com.epam.training.microservicefoundation.resourceservice.service.ResourceService;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
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
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Component
public class ResourceHandler {
  private final ResourceService service;

  @Autowired
  public ResourceHandler(ResourceService service) {
    this.service = service;
  }

  public Mono<ServerResponse> save(ServerRequest request) {
    Mono<MultiValueMap<String, Part>> multiValueMapMono = request.body(BodyExtractors.toMultipartData());
    Mono<FilePart> filePart = multiValueMapMono.flatMap(map -> Mono.justOrEmpty(map.getFirst("file"))).ofType(FilePart.class);

    return service.save(filePart)
        .flatMap(resourceRecord -> ServerResponse.created(URI.create(request.path() + "/" + resourceRecord.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(resourceRecord));
  }

  public Mono<ServerResponse> deleteByIds(ServerRequest request) {
    Flux<Long> idsFlux = request
        .queryParam("id")
        .map(string -> Flux.fromArray(string.split(",")).map(Long::parseLong))
        .orElse(Flux.empty());

    return service.deleteByIds(idsFlux)
        .collectList()
        .flatMap(resourceRecords -> ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(resourceRecords));
  }

  public Mono<ServerResponse> getById(ServerRequest request) {
    long id = Long.parseLong(request.pathVariable("id"));
    return service.getById(id)
        .flatMap(responsePublisher -> ServerResponse.ok()
            .header(HttpHeaders.CONTENT_TYPE, responsePublisher.response().contentType())
            .header(HttpHeaders.CONTENT_LENGTH, Long.toString(responsePublisher.response().contentLength()))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + getMetadataItem(responsePublisher.response(),
                "filename", "UNKNOWN") + "\"")
            .body(Flux.from(responsePublisher), ByteBuffer.class));
  }

  private String getMetadataItem(GetObjectResponse sdkResponse, String key, String defaultValue) {
    for (Map.Entry<String, String> entry : sdkResponse.metadata()
        .entrySet()) {
      if (entry.getKey()
          .equalsIgnoreCase(key)) {
        return entry.getValue();
      }
    }
    return defaultValue;
  }
}
