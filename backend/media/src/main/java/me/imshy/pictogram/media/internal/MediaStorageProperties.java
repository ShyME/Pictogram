package me.imshy.pictogram.media.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("pictogram.media.storage")
record MediaStorageProperties(String endpoint, String region, String accessKey, String secretKey, String bucket,
    boolean pathStyleAccess) {
}
