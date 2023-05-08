package com.epam.training.microservicefoundation.resourceservice.service;

import com.epam.training.microservicefoundation.resourceservice.model.ResourceRecord;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.util.List;

public interface ResourceService {
    Mono<ResourceRecord> save(Mono<FilePart> file);
    Mono<ResponsePublisher<GetObjectResponse>> getById(long id);
    Flux<ResourceRecord> deleteByIds(Flux<Long> ids);
}
