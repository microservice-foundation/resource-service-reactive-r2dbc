package com.epam.training.microservicefoundation.resourceservice.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

@TestConfiguration
@EnableR2dbcRepositories
@EnableR2dbcAuditing
public class DatasourceConfiguration {
  @Bean
  public ConnectionFactoryInitializer initializer(ConnectionFactory factory) {
    ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
    initializer.setConnectionFactory(factory);

    CompositeDatabasePopulator populator = new CompositeDatabasePopulator();
    populator.addPopulators(new ResourceDatabasePopulator(new ClassPathResource("db/schema.sql")));
    populator.addPopulators(new ResourceDatabasePopulator(new ClassPathResource("db/data.sql")));
    initializer.setDatabasePopulator(populator);
    return initializer;
  }
}
