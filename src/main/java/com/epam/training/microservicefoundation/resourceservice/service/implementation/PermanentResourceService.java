package com.epam.training.microservicefoundation.resourceservice.service.implementation;

import com.epam.training.microservicefoundation.resourceservice.domain.context.ResourcePermanentContext;
import com.epam.training.microservicefoundation.resourceservice.domain.dto.StorageType;
import com.epam.training.microservicefoundation.resourceservice.domain.entity.Resource;
import com.epam.training.microservicefoundation.resourceservice.domain.entity.ResourceStatus;
import com.epam.training.microservicefoundation.resourceservice.domain.event.ResourceProcessedEvent;
import com.epam.training.microservicefoundation.resourceservice.domain.exception.ExceptionSupplier;
import com.epam.training.microservicefoundation.resourceservice.repository.CloudStorageRepository;
import com.epam.training.microservicefoundation.resourceservice.repository.ResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@Transactional(readOnly = true)
public class PermanentResourceService {
  private static final Logger log = LoggerFactory.getLogger(PermanentResourceService.class);
  private final StorageManager storageManager;
  private final ResourceRepository resourceRepository;
  private final CloudStorageRepository cloudStorageRepository;

  public PermanentResourceService(StorageManager storageManager, ResourceRepository resourceRepository,
      CloudStorageRepository cloudStorageRepository) {
    this.storageManager = storageManager;
    this.resourceRepository = resourceRepository;
    this.cloudStorageRepository = cloudStorageRepository;
  }

  @Transactional
  public Mono<Void> saveToPermanent(final ResourceProcessedEvent event) {
    log.info("Moving resource with id = {} to permanent.", event.getId());
    return resourceRepository.findById(event.getId())
        .map(resource -> new ResourcePermanentContext().withResource(resource))
        .flatMap(this::getStorage)
        .flatMap(this::getDestinationStorage)
        .flatMap(this::moveToPermanentStorage)
        .flatMap(this::saveProcessedResource);
  }

  private Mono<ResourcePermanentContext> getStorage(final ResourcePermanentContext context) {
    return storageManager.getById(context.getResource().getStorageId()).map(context::withSourceStorage);
  }

  private Mono<ResourcePermanentContext> getDestinationStorage(final ResourcePermanentContext context) {
    return storageManager.getByType(StorageType.PERMANENT)
        .map(context::withDestinationStorage);
  }

  private Mono<ResourcePermanentContext> moveToPermanentStorage(final ResourcePermanentContext context) {
    return cloudStorageRepository.move(context.getResource().getKey(), context.getSourceStorage(), context.getDestinationStorage())
        .map(key -> context.withResource(context.getResource().toBuilder().key(key).build()));
  }

  private Mono<Void> saveProcessedResource(final ResourcePermanentContext context) {
    final Resource resource = context.getResource().toBuilder()
        .status(ResourceStatus.PROCESSED)
        .storageId(context.getDestinationStorage().getId())
        .build();

    return resourceRepository.save(resource)
        .onErrorMap(TransientDataAccessException.class, error -> ExceptionSupplier.entityNotFound(Resource.class, resource.getId()).get())
        .then();
  }
}
