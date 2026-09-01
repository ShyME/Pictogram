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

/**
 * Deletes media that no post references once they are older than the grace period — the row
 * and both renditions. An upload only becomes a candidate after the grace period so a
 * composer that is still writing its caption keeps its image; a media whose post was later
 * deleted becomes a candidate again the moment {@link PostReferences} stops naming it.
 *
 * <p>Bytes go before the row. A crash in between leaves a row whose bytes are gone — its URL
 * 500s until the next sweep clears the row, which no one hits because nothing references it.
 * The reverse (row gone, bytes kept) would leak the bytes forever: no later sweep can see
 * them. Each media is deleted on its own, so one storage error doesn't strand the rest.
 *
 * <p>A concurrent publish that attaches a past-grace media between the reference check and
 * its deletion would lose the bytes; the grace period is what makes that near-impossible
 * (a just-uploaded image is never past grace, and reusing a day-old upload is not a path
 * the UI offers). No foreign key can enforce it — contexts share no schema (ADR-0002).
 */
@Service
class OrphanCollector implements OrphanCollection {

    private static final Logger log = LoggerFactory.getLogger(OrphanCollector.class);

    /** One sweep's bite. A backlog drains over consecutive runs rather than in one long scan. */
    private static final Limit BATCH = Limit.of(500);

    private final Medias medias;
    private final BlobStore blobs;
    private final PostReferences postReferences;
    private final Clock clock;
    private final Duration gracePeriod;

    OrphanCollector(Medias medias, BlobStore blobs, PostReferences postReferences, Clock clock,
            MediaRetentionProperties retention) {
        this.medias = medias;
        this.blobs = blobs;
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
            blobs.remove(StorageKeys.original(mediaId));
            blobs.remove(StorageKeys.thumbnail(mediaId));
            medias.delete(media);
            return true;
        } catch (RuntimeException e) {
            log.warn("Orphan media sweep could not remove {}, leaving it for the next run", mediaId, e);
            return false;
        }
    }
}
