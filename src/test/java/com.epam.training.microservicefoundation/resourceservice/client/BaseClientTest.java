package com.epam.training.microservicefoundation.resourceservice.client;

import com.epam.training.microservicefoundation.resourceservice.config.ClientConfiguration;
import com.epam.training.microservicefoundation.resourceservice.config.properties.WebClientProperties;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.config.client.RetryProperties;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@DirtiesContext
@ExtendWith({MockServerExtension.class})
@EnableConfigurationProperties({WebClientProperties.class, RetryProperties.class})
@ContextConfiguration(classes = ClientConfiguration.class)
@TestPropertySource(locations = "classpath:application.properties")
public abstract class BaseClientTest {
}
