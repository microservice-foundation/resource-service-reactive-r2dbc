package com.epam.training.microservicefoundation.resourceservice.service.implementation;

import com.epam.training.microservicefoundation.resourceservice.model.Mapper;
import com.epam.training.microservicefoundation.resourceservice.model.Resource;
import com.epam.training.microservicefoundation.resourceservice.model.ResourceRecord;
import com.epam.training.microservicefoundation.resourceservice.model.exception.ResourceNotFoundException;
import com.epam.training.microservicefoundation.resourceservice.repository.CloudStorageRepository;
import com.epam.training.microservicefoundation.resourceservice.repository.ResourceRepository;
import com.epam.training.microservicefoundation.resourceservice.service.ResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Service
@Transactional(readOnly = true)
public class ResourceServiceImpl implements ResourceService {

  private static final Logger log = LoggerFactory.getLogger(ResourceServiceImpl.class);
  private final ResourceRepository resourceRepository;
  private final CloudStorageRepository storageRepository;
  private final Mapper<Resource, ResourceRecord> mapper;
  private final KafkaManager kafkaManager;

  @Autowired
  public ResourceServiceImpl(ResourceRepository resourceRepository, CloudStorageRepository storageRepository,
      Mapper<Resource, ResourceRecord> mapper, KafkaManager kafkaManager) {

    this.resourceRepository = resourceRepository;
    this.storageRepository = storageRepository;
    this.mapper = mapper;
    this.kafkaManager = kafkaManager;
  }

  @Transactional
  @Override
  public Mono<ResourceRecord> save(Mono<FilePart> file) {
    log.info("Saving file");
    return file
        .switchIfEmpty(Mono.error(new IllegalArgumentException("File is not validated, please check your file")))
        .zipWhen(storageRepository::upload)
        .map(tuple -> new Resource.Builder(tuple.getT2(), tuple.getT1().filename()).build())
        .flatMap(resourceRepository::save)
        .map(mapper::mapToRecord)
        .flatMap(resourceRecord -> kafkaManager.publish(resourceRecord).thenReturn(resourceRecord));
  }

  @Override
  public Mono<ResponsePublisher<GetObjectResponse>> getById(long id) {
    log.info("Getting file by id '{}'", id);
    return resourceRepository.findById(id)
        .switchIfEmpty(Mono.error(new ResourceNotFoundException(String.format("Resource is not found with id '%d'", id))))
        .flatMap(resource -> storageRepository.getByFileKey(resource.getKey()));
  }

  @Transactional
  @Override
  public Flux<ResourceRecord> deleteByIds(Flux<Long> ids) {
    log.info("Deleting file(s) by id(s)");
    return ids.switchIfEmpty(Mono.error(new IllegalArgumentException("Id param is not validated, check your ids")))
        .flatMap(resourceRepository::findById)
        .flatMap(resource -> storageRepository.deleteByFileKey(resource.getKey()).thenReturn(resource))
        .flatMap(resource -> resourceRepository.delete(resource).thenReturn(resource))
        .map(resource -> new ResourceRecord(resource.getId()));
  }
}
