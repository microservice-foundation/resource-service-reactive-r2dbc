package com.epam.training.microservicefoundation.resourceservice.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource(value = "classpath:resource-query.properties")
public final class ResourceSQL {
    @Value("${song.resource.update}")
    private String update;
    @Value("${song.resource.find.by.id}")
    private String findById;
    @Value("${song.resource.find.name.by.id}")
    private String findNameById;
    @Value("${song.resource.delete}")
    private String delete;

    @Value("${song.resource.exists.by.id}")
    private String existsById;
    @Value("${song.resource.table}")
    private String table;

    @Value("${song.resource.generated.key}")
    private String generatedKey;

    public ResourceSQL() {
        // TODO document why this constructor is empty
    }

    public String getUpdate() {
        return update;
    }

    public String getFindById() {
        return findById;
    }

    public String getDelete() {
        return delete;
    }

    public String getTable() {
        return table;
    }

    public String getGeneratedKey() { return generatedKey; }

    public String getFindNameById() {
        return findNameById;
    }

    public String getExistsById() {
        return existsById;
    }
}
