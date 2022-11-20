package com.epam.training.microservicefoundation.resourceservice.domain;

import org.springframework.stereotype.Component;

@Component
public class ResourceMapper implements Mapper<Resource, ResourceRecord>{

    @Override
    public ResourceRecord mapToRecord(Resource resource) {
        if(resource == null)
            return null;

        return new ResourceRecord(resource.getId());
    }
}
