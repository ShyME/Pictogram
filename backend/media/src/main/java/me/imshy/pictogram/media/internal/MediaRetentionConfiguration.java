package me.imshy.pictogram.media.internal;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Binds {@link MediaRetentionProperties}, matching {@code MediaStorageConfiguration}. */
@Configuration
@EnableConfigurationProperties(MediaRetentionProperties.class)
class MediaRetentionConfiguration {
}
