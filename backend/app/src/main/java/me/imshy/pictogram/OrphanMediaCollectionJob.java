package me.imshy.pictogram;

import me.imshy.pictogram.media.OrphanCollection;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs media's orphan sweep on a cron ({@code pictogram.media.retention.collection-cron},
 * hourly by default; {@code "-"} disables it). The grace period that decides what is
 * eligible is media's own ({@code pictogram.media.retention.grace-period}).
 */
@Component
class OrphanMediaCollectionJob {

    private final OrphanCollection orphanCollection;

    OrphanMediaCollectionJob(OrphanCollection orphanCollection) {
        this.orphanCollection = orphanCollection;
    }

    @Scheduled(cron = "${pictogram.media.retention.collection-cron}")
    void sweep() {
        orphanCollection.collectOrphans();
    }
}
