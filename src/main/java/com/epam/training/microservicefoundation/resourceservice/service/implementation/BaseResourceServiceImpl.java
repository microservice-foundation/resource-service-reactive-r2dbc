package com.epam.training.microservicefoundation.resourceservice.service.implementation;

import com.epam.training.microservicefoundation.resourceservice.service.mapper.DeleteResourceMapper;
import com.epam.training.microservicefoundation.resourceservice.domain.context.BaseContext;
import com.epam.training.microservicefoundation.resourceservice.domain.dto.DeleteResourceDTO;
import com.epam.training.microservicefoundation.resourceservice.domain.entity.Resource;
import com.epam.training.microservicefoundation.resourceservice.domain.exception.ExceptionSupplier;
import com.epam.training.microservicefoundation.resourceservice.repository.CloudStorageRepository;
import com.epam.training.microservicefoundation.resourceservice.repository.ResourceRepository;
import com.epam.training.microservicefoundation.resourceservice.service.BaseResourceService;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Service
@Transactional(readOnly = true)
public class BaseResourceServiceImpl implements BaseResourceService {
  private static final Logger log = LoggerFactory.getLogger(BaseResourceServiceImpl.class);
  private final ResourceRepository resourceRepository;
  private final CloudStorageRepository cloudStorageRepository;
  private final StorageManager storageManager;
  private final DeleteResourceMapper deleteResourceMapper;

  @Autowired
  public BaseResourceServiceImpl(ResourceRepository resourceRepository, CloudStorageRepository cloudStorageRepository,
      StorageManager storageManager, DeleteResourceMapper deleteResourceMapper) {

    this.resourceRepository = resourceRepository;
    this.cloudStorageRepository = cloudStorageRepository;
    this.storageManager = storageManager;
    this.deleteResourceMapper = deleteResourceMapper;
  }

  @Override
  public Mono<ResponsePublisher<GetObjectResponse>> getById(final long id) {
    log.info("Getting file by id '{}'.", id);
    return resourceRepository.findById(id)
        .map(BaseContext::new)
        .flatMap(this::getStorage)
        .flatMap(context -> cloudStorageRepository.getByKey(context.getResource().getKey(), context.getStorage().getBucket()))
        .switchIfEmpty(Mono.error(ExceptionSupplier.entityNotFound(Resource.class, id)));
  }

  @Transactional
  @Override
  public Flux<DeleteResourceDTO> deleteByIds(final Long[] ids) {
    log.info("Deleting file(s) by id(s) '{}'", Arrays.toString(ids));
    return Flux.fromArray(ids)
        .flatMap(resourceRepository::findById)
        .map(BaseContext::new)
        .flatMap(this::getStorage)
        .flatMap(context -> cloudStorageRepository.deleteByKey(context.getResource().getKey(), context.getStorage().getBucket())
            .thenReturn(context))
        .flatMap(context -> resourceRepository.delete(context.getResource()).thenReturn(context))
        .map(context -> deleteResourceMapper.toDto(context.getResource()));
  }

  private Mono<BaseContext> getStorage(final BaseContext context) {
    return storageManager.getById(context.getResource().getStorageId()).map(context::withStorage);
  }
}
