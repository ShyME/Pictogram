package me.imshy.pictogram.media.internal;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import me.imshy.pictogram.media.OrphanCollection;
import me.imshy.pictogram.media.PostReferences;
import me.imshy.pictogram.shared.MediaId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

@Service
class OrphanCollector implements OrphanCollection {

    private static final Logger log = LoggerFactory.getLogger(OrphanCollector.class);

    private static final Limit BATCH = Limit.of(500);

    private final Medias medias;
    private final BlobStore blobStore;
    private final PostReferences postReferences;
    private final Clock clock;
    private final Duration gracePeriod;

    OrphanCollector(Medias medias, BlobStore blobStore, PostReferences postReferences, Clock clock,
        MediaRetentionProperties retention) {
        this.medias = medias;
        this.blobStore = blobStore;
        this.postReferences = postReferences;
        this.clock = clock;
        this.gracePeriod = retention.gracePeriod();
    }

    @Override
    public int collectOrphans() {
        Instant cutoff = clock.instant().minus(gracePeriod);
        List<Media> candidates = medias.uploadedBefore(cutoff, BATCH);
        if (candidates.isEmpty()) {
            return 0;
        }

        Set<MediaId> referenced = postReferences.referencedAmong(candidates.stream().map(Media::mediaId).toList());
        int collected = 0;
        for (Media media : candidates) {
            if (referenced.contains(media.mediaId())) {
                continue;
            }
            if (delete(media)) {
                collected++;
            }
        }

        if (collected > 0) {
            log.info("Orphan media sweep removed {} of {} candidate media", collected, candidates.size());
        }
        return collected;
    }

    private boolean delete(Media media) {
        MediaId mediaId = media.mediaId();
        try {
            blobStore.remove(StorageKeys.original(mediaId));
            blobStore.remove(StorageKeys.thumbnail(mediaId));
            medias.delete(media);
            return true;
        } catch (RuntimeException e) {
            log.warn("Orphan media sweep could not remove {}, leaving it for the next run", mediaId, e);
            return false;
        }
    }
}
