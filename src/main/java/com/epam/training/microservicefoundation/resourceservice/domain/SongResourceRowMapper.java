package com.epam.training.microservicefoundation.resourceservice.domain;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class SongResourceRowMapper implements RowMapper<Resource> {
    @Override
    public Resource mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        long id = resultSet.getLong("id");
        String path = resultSet.getString("path");
        String name = resultSet.getString("name");
        LocalDateTime createdDate = resultSet.getObject("created_date", LocalDateTime.class);
        LocalDateTime lastModifiedDate = resultSet.getObject("last_modified_date", LocalDateTime.class);

        return new Resource.Builder(path, name)
                .id(id)
                .createdDate(createdDate)
                .lastModifiedDate(lastModifiedDate)
                .build();
    }
}
