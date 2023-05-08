package com.epam.training.microservicefoundation.resourceservice.base;

import com.epam.training.microservicefoundation.resourceservice.configuration.AwsS3Configuration;
import com.epam.training.microservicefoundation.resourceservice.configuration.DatasourceConfiguration;
import com.epam.training.microservicefoundation.resourceservice.configuration.KafkaConfiguration;
import com.epam.training.microservicefoundation.resourceservice.configuration.KafkaTopicConfiguration;
import com.epam.training.microservicefoundation.resourceservice.configuration.WebFluxConfiguration;
import com.epam.training.microservicefoundation.resourceservice.repository.resourcedatabase.PostgresExtension;
import com.epam.training.microservicefoundation.resourceservice.repository.s3storage.CloudStorageExtension;
import com.epam.training.microservicefoundation.resourceservice.service.kafka.KafkaExtension;
import io.restassured.module.webtestclient.RestAssuredWebTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ExtendWith(value = {PostgresExtension.class, KafkaExtension.class, CloudStorageExtension.class})
@DirtiesContext
@ContextConfiguration(classes = {WebFluxConfiguration.class, DatasourceConfiguration.class, AwsS3Configuration.class,
    KafkaConfiguration.class, KafkaTopicConfiguration.class})
@TestPropertySource(locations = "classpath:application.properties")
public abstract class RestBase {
  @BeforeEach
  public void setup(ApplicationContext context) {
    RestAssuredWebTestClient.applicationContextSetup(context);
  }
}
