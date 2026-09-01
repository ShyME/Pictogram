package me.imshy.pictogram;

import me.imshy.pictogram.media.OrphanCollection;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
