package com.epam.training.microservicefoundation.resourceservice.api;

import com.epam.training.microservicefoundation.resourceservice.domain.ResourceRecord;
import com.epam.training.microservicefoundation.resourceservice.service.ResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resources")
public class ResourceController {
    //TODO: use swagger
    private static final Logger log = LoggerFactory.getLogger(ResourceController.class);
    @Autowired
    private ResourceService service;

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(value = HttpStatus.CREATED)
    public ResourceRecord save(@RequestParam MultipartFile multipartFile) {
        log.info("Saving multipart file '{}'", multipartFile.getName());
        return service.save(multipartFile);
    }

    @DeleteMapping
    @ResponseStatus(value = HttpStatus.OK)
    public List<ResourceRecord> delete(@RequestParam(value = "id") long[] ids) {
        log.info("Deleting resource(s) with id {}", ids);
        return service.deleteByIds(ids);
    }

    @GetMapping(value = "/{id}", produces = "audio/mpeg")
    @ResponseStatus(value = HttpStatus.OK)
    public InputStreamResource get(@PathVariable long id) {
        return service.getById(id);
    }
}
