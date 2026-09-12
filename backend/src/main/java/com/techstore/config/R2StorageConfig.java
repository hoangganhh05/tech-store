package com.techstore.config;

import java.net.URI;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
@Profile("prod")
@EnableConfigurationProperties(R2StorageProperties.class)
public class R2StorageConfig {

    @Bean
    public S3Client r2S3Client(R2StorageProperties properties) {
        require("R2_ENDPOINT", properties.endpoint());
        require("R2_REGION", properties.region());
        require("R2_BUCKET_NAME", properties.bucket());
        require("R2_ACCESS_KEY_ID", properties.accessKeyId());
        require("R2_SECRET_ACCESS_KEY", properties.secretAccessKey());
        require("R2_PUBLIC_BASE_URL", properties.publicBaseUrl());

        URI endpoint = URI.create(properties.endpoint());
        if (!"https".equalsIgnoreCase(endpoint.getScheme())
                && !"http".equalsIgnoreCase(endpoint.getScheme())) {
            throw new IllegalStateException("R2_ENDPOINT must use HTTP or HTTPS");
        }

        URI publicBaseUrl = URI.create(properties.publicBaseUrl());
        if (!"https".equalsIgnoreCase(publicBaseUrl.getScheme())) {
            throw new IllegalStateException("R2_PUBLIC_BASE_URL must use HTTPS in production");
        }

        return S3Client.builder()
                .endpointOverride(endpoint)
                .region(Region.of(properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.accessKeyId(), properties.secretAccessKey())))
                // R2's S3 endpoint is account-scoped; path-style addressing keeps
                // the bucket name on the request path as required by R2.
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    private static void require(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required production storage configuration: " + name);
        }
    }
}
