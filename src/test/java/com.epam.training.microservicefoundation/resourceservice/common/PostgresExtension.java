package com.epam.training.microservicefoundation.resourceservice.common;

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

    System.setProperty("POSTGRES_HOST", database.getHost());
    System.setProperty("POSTGRES_PORT", String.valueOf(database.getFirstMappedPort()));
    System.setProperty("DATABASE_NAME", database.getDatabaseName());
    System.setProperty("POSTGRES_USERNAME", database.getUsername());
    System.setProperty("POSTGRES_PASSWORD", database.getPassword());
  }
}
