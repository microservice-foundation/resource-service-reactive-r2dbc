package com.epam.training.microservicefoundation.resourceservice.repository;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public final class PostgresExtension implements BeforeAllCallback, AfterAllCallback {
  private PostgreSQLContainer<?> database;

  @Override
  public void afterAll(ExtensionContext context) {
    database.stop();
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    database = new PostgreSQLContainer<>("postgres:12.9-alpine");
    database.start();

    System.setProperty("spring.r2dbc.url", r2dbcUrl(database));
    System.setProperty("spring.r2dbc.username", database.getUsername());
    System.setProperty("spring.r2dbc.password", database.getPassword());
  }

  private String r2dbcUrl(PostgreSQLContainer<?> database) {
    return String.format("r2dbc:postgresql://%s:%s/%s",
        database.getHost(),
        database.getFirstMappedPort(),
        database.getDatabaseName());
  }
}
