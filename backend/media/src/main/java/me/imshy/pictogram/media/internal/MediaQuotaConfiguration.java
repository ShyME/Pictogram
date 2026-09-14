package me.imshy.pictogram.media.internal;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MediaQuotaProperties.class)
class MediaQuotaConfiguration {
}
