package com.techstore.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PasswordResetProperties.class)
public class PasswordResetConfig {
}
