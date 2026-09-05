package me.imshy.pictogram.media.internal;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("pictogram.media.retention")
record MediaRetentionProperties(@DefaultValue("24h") Duration gracePeriod) {
}
