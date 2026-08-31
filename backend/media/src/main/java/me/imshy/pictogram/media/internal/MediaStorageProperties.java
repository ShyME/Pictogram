package me.imshy.pictogram.media.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connection details for the object store under {@code pictogram.media.storage}.
 * {@code pathStyleAccess} is {@code true} for MinIO (it has no virtual-host bucket routing);
 * a real AWS S3 endpoint can leave it {@code false}.
 */
@ConfigurationProperties("pictogram.media.storage")
record MediaStorageProperties(
        String endpoint,
        String region,
        String accessKey,
        String secretKey,
        String bucket,
        boolean pathStyleAccess) {
}
