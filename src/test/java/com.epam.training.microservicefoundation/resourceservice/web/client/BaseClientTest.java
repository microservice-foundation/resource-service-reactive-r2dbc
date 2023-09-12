package com.epam.training.microservicefoundation.resourceservice.web.client;

import com.epam.training.microservicefoundation.resourceservice.configuration.ClientConfiguration;
import com.epam.training.microservicefoundation.resourceservice.configuration.properties.WebClientProperties;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.config.client.RetryProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith({SpringExtension.class, MockServerExtension.class})
@ContextConfiguration(classes = ClientConfiguration.class)
@TestPropertySource(locations = "classpath:application.properties")
public abstract class BaseClientTest {
}
