package com.techstore.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StoreBrandProperties.class)
public class StoreBrandConfig {
}
