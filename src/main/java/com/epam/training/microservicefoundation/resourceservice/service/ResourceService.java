package com.epam.training.microservicefoundation.resourceservice.service;

import com.epam.training.microservicefoundation.resourceservice.model.ResourceDTO;
import com.epam.training.microservicefoundation.resourceservice.model.ResourceProcessedEvent;
import com.epam.training.microservicefoundation.resourceservice.model.ResourceStagedEvent;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public interface ResourceService {
  Mono<ResourceDTO> save(Mono<FilePart> file);
  Mono<ResponsePublisher<GetObjectResponse>> getById(long id);
  Flux<ResourceDTO> deleteByIds(Flux<Long> ids);
  Mono<Void> moveToPermanent(ResourceProcessedEvent event);
}
