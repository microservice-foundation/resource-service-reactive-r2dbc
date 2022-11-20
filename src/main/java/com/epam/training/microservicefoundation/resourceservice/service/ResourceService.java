package com.epam.training.microservicefoundation.resourceservice.service;

import com.epam.training.microservicefoundation.resourceservice.domain.ResourceRecord;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResourceService {
    ResourceRecord save(MultipartFile file);
    InputStreamResource getById(long id);
    List<ResourceRecord> deleteByIds(long[] ids);
}
