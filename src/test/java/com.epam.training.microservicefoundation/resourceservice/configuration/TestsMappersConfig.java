package com.epam.training.microservicefoundation.resourceservice.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(basePackages = "com.epam.training.microservicefoundation.resourceservice.service.mapper",
    includeFilters = {@Filter(type = FilterType.REGEX, pattern = "\\*Impl")})
public class TestsMappersConfig { }
