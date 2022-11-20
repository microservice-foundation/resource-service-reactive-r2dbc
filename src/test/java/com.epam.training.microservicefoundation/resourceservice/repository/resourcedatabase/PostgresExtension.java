package com.epam.training.microservicefoundation.resourceservice.repository.resourcedatabase;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public final class PostgresExtension implements BeforeAllCallback, AfterAllCallback {
    private PostgreSQLContainer<?> database;

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        database = new PostgreSQLContainer<>("postgres:12.9-alpine");
        database.start();

        System.setProperty("spring.test.database.replace", "none");
        System.setProperty("spring.datasource.url", database.getJdbcUrl());
        System.setProperty("spring.datasource.username", database.getUsername());
        System.setProperty("spring.datasource.password", database.getPassword());
    }
}
