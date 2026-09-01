package me.imshy.pictogram.media.internal;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * How long an unreferenced media is kept before the orphan sweep may delete it, under
 * {@code pictogram.media.retention}. The grace period covers the gap between an upload and
 * the publish that references it — a composer mid-caption must not have its image swept out
 * from under it. The schedule itself is the composition root's {@code @Scheduled} cron.
 */
@ConfigurationProperties("pictogram.media.retention")
record MediaRetentionProperties(@DefaultValue("24h") Duration gracePeriod) {
}
