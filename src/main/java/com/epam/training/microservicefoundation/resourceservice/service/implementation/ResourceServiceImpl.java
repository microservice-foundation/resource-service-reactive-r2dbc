package com.epam.training.microservicefoundation.resourceservice.service.implementation;

import com.epam.training.microservicefoundation.resourceservice.kafka.producer.KafkaProducer;
import com.epam.training.microservicefoundation.resourceservice.model.Mapper;
import com.epam.training.microservicefoundation.resourceservice.model.Resource;
import com.epam.training.microservicefoundation.resourceservice.model.ResourceDTO;
import com.epam.training.microservicefoundation.resourceservice.model.ResourceFile;
import com.epam.training.microservicefoundation.resourceservice.model.ResourcePermanentContext;
import com.epam.training.microservicefoundation.resourceservice.model.ResourceProcessedEvent;
import com.epam.training.microservicefoundation.resourceservice.model.ResourceStagedEvent;
import com.epam.training.microservicefoundation.resourceservice.model.ResourceStagingContext;
import com.epam.training.microservicefoundation.resourceservice.model.ResourceStatus;
import com.epam.training.microservicefoundation.resourceservice.model.StorageType;
import com.epam.training.microservicefoundation.resourceservice.model.exception.ResourceNotFoundException;
import com.epam.training.microservicefoundation.resourceservice.repository.CloudStorageRepository;
import com.epam.training.microservicefoundation.resourceservice.repository.ResourceRepository;
import com.epam.training.microservicefoundation.resourceservice.service.ResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Service
@Transactional(readOnly = true)
public class ResourceServiceImpl implements ResourceService {
  private static final Logger log = LoggerFactory.getLogger(ResourceServiceImpl.class);
  private final ResourceRepository resourceRepository;
  private final CloudStorageRepository storageRepository;
  private final Mapper<Resource, ResourceDTO> mapper;
  private final KafkaProducer kafkaProducer;
  private final StorageManager storageManager;

  @Autowired
  public ResourceServiceImpl(ResourceRepository resourceRepository, CloudStorageRepository storageRepository,
      Mapper<Resource, ResourceDTO> mapper, KafkaProducer kafkaProducer, StorageManager storageManager) {

    this.resourceRepository = resourceRepository;
    this.storageRepository = storageRepository;
    this.mapper = mapper;
    this.kafkaProducer = kafkaProducer;
    this.storageManager = storageManager;
  }

  @Transactional
  @Override
  public Mono<ResourceDTO> save(Mono<FilePart> file) {
    log.info("Saving file {}", file);
    return file
        .map(filePart -> new ResourceStagingContext().withFilePart(filePart))
        .flatMap(this::refuseIfResourceExistsByName)
        .flatMap(this::getStagingStorage)
        .flatMap(this::uploadResourceFile)
        .flatMap(this::saveStagedResource)
        .flatMap(this::publishResourceStagingEvent)
        .map(context -> mapper.mapToRecord(context.getResource()))
        .switchIfEmpty(Mono.error(new IllegalArgumentException("File is not validated, please check your file")));
  }

  private Mono<ResourceStagingContext> refuseIfResourceExistsByName(ResourceStagingContext context) {
    return resourceRepository.existsByName(context.getFilePart().filename())
        .filter(result -> Boolean.FALSE.equals(result) || !StringUtils.hasLength(context.getFilePart().filename()))
        .map(result -> context);
  }

  private Mono<ResourceStagingContext> uploadResourceFile(ResourceStagingContext context) {
    final ResourceFile resourceFile = new ResourceFile(context.getFilePart(), context.getStorage());
    return storageRepository.upload(resourceFile)
        .map(result -> context.withResource(Resource.builder().key(result.getKey()).name(result.getFilename()).build()));
  }

  private Mono<ResourceStagingContext> publishResourceStagingEvent(ResourceStagingContext context) {
    return kafkaProducer.publish(new ResourceStagedEvent(context.getResource().getId())).thenReturn(context);
  }

  private Mono<ResourceStagingContext> saveStagedResource(ResourceStagingContext context) {
    Resource resource = context.getResource().toBuilder()
        .status(ResourceStatus.STAGED)
        .storageId(context.getStorage().getId())
        .build();

    return resourceRepository.save(resource)
        .map(context::withResource)
        .onErrorMap(DataIntegrityViolationException.class, e -> new IllegalArgumentException(String.format(
            "Saving a storage record with invalid parameters length or duplicate value '%s'", e.getLocalizedMessage()), e))
        .doOnError(error -> storageRepository.deleteByKey(resource.getKey(), context.getStorage().getBucket()));
  }

  private Mono<ResourceStagingContext> getStagingStorage(ResourceStagingContext context) {
    log.info("Getting a storage by type '{}'", StorageType.STAGING);
    return storageManager.getByType(StorageType.STAGING)
        .map(context::withStorage);
  }

  @Override
  public Mono<ResponsePublisher<GetObjectResponse>> getById(long id) {
    log.info("Getting file by id '{}'", id);
    return resourceRepository.findById(id)
        .zipWhen(resource -> storageManager.getById(resource.getStorageId()))
        .flatMap(tuple -> storageRepository.getByKey(tuple.getT1().getKey(), tuple.getT2().getBucket()))
        .switchIfEmpty(Mono.error(new ResourceNotFoundException(String.format("Resource is not found with id '%d'", id))));
  }

  @Transactional
  @Override
  public Flux<ResourceDTO> deleteByIds(Flux<Long> ids) {
    log.info("Deleting file(s) by id(s)");
    return ids
        .flatMap(resourceRepository::findById)
        .flatMap(resource -> storageManager.getById(resource.getStorageId()).map(storage -> Tuples.of(resource, storage)))
        .flatMap(tuple -> storageRepository.deleteByKey(tuple.getT1().getKey(), tuple.getT2().getBucket()).thenReturn(tuple.getT1()))
        .flatMap(resource -> resourceRepository.delete(resource).thenReturn(resource))
        .map(mapper::mapToRecord)
        .switchIfEmpty(Mono.error(new IllegalArgumentException("Id param is not validated, check your ids")));
  }

  @Transactional
  @Override
  public Mono<Void> moveToPermanent(ResourceProcessedEvent event) {
    log.info("Moving resource with id = {} to permanent", event.getId());
    return resourceRepository.findById(event.getId())
        .map(resource -> new ResourcePermanentContext().withResource(resource))
        .flatMap(this::getSourceStorage)
        .flatMap(this::getDestinationStorage)
        .flatMap(this::moveToPermanentStorage)
        .flatMap(this::saveProcessedResource);
  }

  private Mono<ResourcePermanentContext> moveToPermanentStorage(ResourcePermanentContext context) {
    return storageRepository.move(context.getResource().getKey(), context.getSourceStorage(), context.getDestinationStorage())
        .map(key -> context.withResource(context.getResource().toBuilder().key(key).build()));
  }

  private Mono<ResourcePermanentContext> getSourceStorage(ResourcePermanentContext context) {
    log.info("Getting a source storage by id = {}", context.getResource().getStorageId());
    return storageManager.getById(context.getResource().getStorageId())
        .map(context::withSourceStorage);
  }

  private Mono<ResourcePermanentContext> getDestinationStorage(ResourcePermanentContext context) {
    log.info("Getting a storage by type '{}' and set it as a destination storage", StorageType.PERMANENT);
    return storageManager.getByType(StorageType.PERMANENT)
        .map(context::withDestinationStorage);
  }

  private Mono<Void> saveProcessedResource(ResourcePermanentContext context) {
    Resource resource = context.getResource().toBuilder()
        .status(ResourceStatus.PROCESSED)
        .storageId(context.getDestinationStorage().getId())
        .build();

    log.info("Saving processed resource '{}'", resource);
    return resourceRepository.save(resource)
        .onErrorMap(TransientDataAccessException.class, error ->
            new ResourceNotFoundException(String.format("Resource does not exist with id=%d", resource.getId()), error)).then();
  }
}
