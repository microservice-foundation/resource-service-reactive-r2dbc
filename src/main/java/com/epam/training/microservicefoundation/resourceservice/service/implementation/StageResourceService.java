package com.epam.training.microservicefoundation.resourceservice.service.implementation;

import com.epam.training.microservicefoundation.resourceservice.kafka.producer.KafkaProducer;
import com.epam.training.microservicefoundation.resourceservice.service.mapper.GetResourceMapper;
import com.epam.training.microservicefoundation.resourceservice.domain.ResourceFile;
import com.epam.training.microservicefoundation.resourceservice.domain.context.ResourceStagingContext;
import com.epam.training.microservicefoundation.resourceservice.domain.dto.GetResourceDTO;
import com.epam.training.microservicefoundation.resourceservice.domain.dto.StorageType;
import com.epam.training.microservicefoundation.resourceservice.domain.entity.Resource;
import com.epam.training.microservicefoundation.resourceservice.domain.entity.ResourceStatus;
import com.epam.training.microservicefoundation.resourceservice.domain.event.ResourceStagedEvent;
import com.epam.training.microservicefoundation.resourceservice.domain.exception.ExceptionSupplier;
import com.epam.training.microservicefoundation.resourceservice.repository.CloudStorageRepository;
import com.epam.training.microservicefoundation.resourceservice.repository.ResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Service
@Transactional(readOnly = true)
public class StageResourceService {
  private static final Logger log = LoggerFactory.getLogger(StageResourceService.class);
  private final ResourceRepository resourceRepository;
  private final CloudStorageRepository cloudStorageRepository;
  private final KafkaProducer kafkaProducer;
  private final StorageManager storageManager;
  private final GetResourceMapper getResourceMapper;

  @Autowired
  public StageResourceService(ResourceRepository resourceRepository, CloudStorageRepository cloudStorageRepository,
      KafkaProducer kafkaProducer, StorageManager storageManager, GetResourceMapper getResourceMapper) {
    this.resourceRepository = resourceRepository;
    this.cloudStorageRepository = cloudStorageRepository;
    this.kafkaProducer = kafkaProducer;
    this.storageManager = storageManager;
    this.getResourceMapper = getResourceMapper;
  }


  @Transactional
  public Mono<GetResourceDTO> saveToStage(Mono<FilePart> file) {
    log.info("Saving file.");
    return file
        .map(filePart -> new ResourceStagingContext().withFilePart(filePart))
        .flatMap(this::refuseIfResourceExistsByName)
        .flatMap(this::getStorage)
        .flatMap(this::uploadResourceFile)
        .flatMap(this::saveStagedResource)
        .flatMap(this::publishResourceStagingEvent)
        .map(context -> getResourceMapper.toDto(context.getResource()))
        .switchIfEmpty(Mono.error(ExceptionSupplier.invalidRequest(FilePart.class).get()));
  }

  private Mono<ResourceStagingContext> refuseIfResourceExistsByName(final ResourceStagingContext context) {
    return resourceRepository.existsByName(context.getFilePart().filename())
        .filter(result -> Boolean.FALSE.equals(result) || !StringUtils.hasLength(context.getFilePart().filename()))
        .map(result -> context);
  }

  private Mono<ResourceStagingContext> uploadResourceFile(final ResourceStagingContext context) {
    final ResourceFile resourceFile = new ResourceFile(context.getFilePart(), context.getStorage());
    return cloudStorageRepository.upload(resourceFile)
        .map(result -> context.withResource(Resource.builder().key(result.getKey()).name(result.getFilename()).build()));
  }

  private Mono<ResourceStagingContext> publishResourceStagingEvent(final ResourceStagingContext context) {
    return kafkaProducer.publish(new ResourceStagedEvent(context.getResource().getId())).thenReturn(context);
  }

  private Mono<ResourceStagingContext> saveStagedResource(final ResourceStagingContext context) {
    final Resource resource = context.getResource().toBuilder()
        .status(ResourceStatus.STAGED)
        .storageId(context.getStorage().getId())
        .build();

    return resourceRepository.save(resource)
        .map(context::withResource)
        .onErrorResume(DataIntegrityViolationException.class, error -> // fallback call to clean up resource in cloud.
            cloudStorageRepository.deleteByKey(resource.getKey(), context.getStorage().getBucket()).then(Mono.error(error)))
        .onErrorMap(DataIntegrityViolationException.class, error -> ExceptionSupplier.entityAlreadyExists(Resource.class, error).get());
  }

  private Mono<ResourceStagingContext> getStorage(final ResourceStagingContext context) {
    return storageManager.getByType(StorageType.STAGING).map(context::withStorage);
  }
}
