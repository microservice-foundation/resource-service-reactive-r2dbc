package com.epam.training.microservicefoundation.resourceservice.service;

import com.epam.training.microservicefoundation.resourceservice.model.dto.DeleteResourceDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public interface BaseResourceService {
  Mono<ResponsePublisher<GetObjectResponse>> getById(final long id);
  Flux<DeleteResourceDTO> deleteByIds(final Long[] ids);
}
