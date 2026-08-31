package me.imshy.pictogram.media.internal;

import java.time.Clock;
import java.util.Optional;
import me.imshy.pictogram.media.MediaCatalog;
import me.imshy.pictogram.media.internal.ImagePipeline.Renditions;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.UserId;
import org.springframework.stereotype.Service;

/**
 * media's one service. Commands: take an upload, re-encode it, store both renditions and
 * record the {@link Media}; read either rendition's bytes back. Query: the published
 * {@link MediaCatalog} — does a media exist, and who owns it. The re-encode is
 * {@link ImagePipeline}'s job and the byte storage is a {@link BlobStore}'s; this class is
 * the orchestration.
 *
 * <p>Renditions are written to the {@link BlobStore} before the row is saved: a crash in
 * between leaves orphan objects with no row, which the orphan-collection job reaps (#15) —
 * the harmless direction. The reverse would leave a row pointing at absent bytes.
 */
@Service
public class MediaLibrary implements MediaCatalog {

    private final Medias medias;
    private final ImagePipeline pipeline;
    private final BlobStore blobs;
    private final Clock clock;

    MediaLibrary(Medias medias, ImagePipeline pipeline, BlobStore blobs, Clock clock) {
        this.medias = medias;
        this.pipeline = pipeline;
        this.blobs = blobs;
        this.clock = clock;
    }

    public MediaId upload(UserId owner, byte[] upload) {
        Renditions renditions = pipeline.transcode(upload);

        Media media = Media.uploadedBy(owner, clock.instant());
        MediaId mediaId = media.mediaId();
        blobs.put(StorageKeys.original(mediaId), renditions.original());
        blobs.put(StorageKeys.thumbnail(mediaId), renditions.thumbnail());
        medias.save(media);
        return mediaId;
    }

    public byte[] original(MediaId mediaId) {
        requireExists(mediaId);
        return blobs.get(StorageKeys.original(mediaId));
    }

    public byte[] thumbnail(MediaId mediaId) {
        requireExists(mediaId);
        return blobs.get(StorageKeys.thumbnail(mediaId));
    }

    @Override
    public boolean exists(MediaId mediaId) {
        return medias.existsById(mediaId.value());
    }

    @Override
    public Optional<UserId> ownerOf(MediaId mediaId) {
        return medias.findById(mediaId.value()).map(Media::ownerId);
    }

    private void requireExists(MediaId mediaId) {
        if (!exists(mediaId)) {
            throw new MediaNotFoundException();
        }
    }
}
