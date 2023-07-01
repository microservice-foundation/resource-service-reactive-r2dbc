package com.epam.training.microservicefoundation.resourceservice.model;

import org.springframework.stereotype.Component;

@Component
public class ResourceMapper implements Mapper<Resource, ResourceDTO> {

    @Override
    public ResourceDTO mapToRecord(Resource resource) {
        if(resource == null)
            return null;
        return new ResourceDTO(resource.getId());
    }
}
