package com.techstore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the S3-compatible Cloudflare R2 object store.
 *
 * Keeping these values in a typed object makes it possible to validate the
 * production configuration without ever logging the access credentials.
 */
@ConfigurationProperties(prefix = "app.storage.r2")
public record R2StorageProperties(
        String endpoint,
        String region,
        String bucket,
        String accessKeyId,
        String secretAccessKey,
        String publicBaseUrl
) {
}
