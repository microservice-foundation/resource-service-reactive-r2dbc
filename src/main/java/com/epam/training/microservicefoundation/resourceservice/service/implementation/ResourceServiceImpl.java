package com.epam.training.microservicefoundation.resourceservice.service.implementation;

import com.epam.training.microservicefoundation.resourceservice.domain.ResourceMapper;
import com.epam.training.microservicefoundation.resourceservice.domain.ResourceNotFoundException;
import com.epam.training.microservicefoundation.resourceservice.domain.ResourceRecord;
import com.epam.training.microservicefoundation.resourceservice.domain.Resource;
import com.epam.training.microservicefoundation.resourceservice.repository.CloudStorageRepository;
import com.epam.training.microservicefoundation.resourceservice.repository.ResourceRepository;
import com.epam.training.microservicefoundation.resourceservice.service.Convertor;
import com.epam.training.microservicefoundation.resourceservice.service.ResourceService;
import com.epam.training.microservicefoundation.resourceservice.service.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ResourceServiceImpl implements ResourceService {

    private static final Logger log = LoggerFactory.getLogger(ResourceServiceImpl.class);
    private final ResourceRepository resourceRepository;
    private final CloudStorageRepository storageRepository;
    private final ResourceMapper mapper;
    private final Validator<MultipartFile> songFileValidator;
    private final Validator<long[]> idParamValidator;
    private final KafkaManager kafkaManager;
    @Autowired
    public ResourceServiceImpl(ResourceRepository resourceRepository, CloudStorageRepository storageRepository,
                               ResourceMapper mapper, MultipartFileValidator songFileValidator,
                               IdParamValidator idParamValidator, KafkaManager kafkaManager) {

        this.resourceRepository = resourceRepository;
        this.storageRepository = storageRepository;
        this.mapper = mapper;
        this.songFileValidator = songFileValidator;
        this.idParamValidator = idParamValidator;
        this.kafkaManager = kafkaManager;
    }

    @Transactional
    @Override
    public ResourceRecord save(MultipartFile file) {
        log.info("Saving file '{}'", file.getOriginalFilename());
        if(!songFileValidator.validate(file)) {
            IllegalArgumentException ex = new IllegalArgumentException(String.format("File with name '%s' was not " +
                    "validated, check your file", file.getOriginalFilename()));

            log.error("File '{}' was not valid to save\nreason:", file.getOriginalFilename(), ex);
            throw ex;
        }

        String path = storageRepository.upload(file);
        Resource resource = new Resource.Builder(path, file.getOriginalFilename())
                .createdDate(LocalDateTime.now())
                .build();

        ResourceRecord resourceRecord = mapper.mapToRecord(resourceRepository.save(resource));
        kafkaManager.publishCallback(resourceRecord);
        return resourceRecord;
    }

    @Override
    public InputStreamResource getById(long id) {
        log.info("Getting file by id '{}'", id);
        String name = resourceRepository.findNameById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Resource not found with id '%d'", id)));

        return new InputStreamResource(storageRepository.getByName(name));
    }

    @Transactional
    @Override
    public List<ResourceRecord> deleteByIds(long[] ids) {
        log.info("Deleting file(s) with id {}", ids);
        if(!idParamValidator.validate(ids)) {
            IllegalArgumentException ex = new IllegalArgumentException("Id param was not validated, check your file");
            log.error("Id param size '{}' should be less than 200 \nreason:", ids.length, ex);
            throw ex;
        }

        Arrays.stream(ids)
                .mapToObj(resourceRepository::findNameById)
                .filter(Optional::isPresent).map(Optional::get)
                .forEach(storageRepository::deleteByName);

        Arrays.stream(ids).forEach(resourceRepository::deleteById);

        log.debug("Resources with id(s) '{}' were deleted", ids);
        return Arrays.stream(ids).mapToObj(ResourceRecord::new).collect(Collectors.toList());
    }

}
