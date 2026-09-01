package me.imshy.pictogram.media.internal;

import java.time.Clock;
import java.util.Optional;
import me.imshy.pictogram.media.MediaCatalog;
import me.imshy.pictogram.media.internal.ImagePipeline.Renditions;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.UserId;
import org.springframework.stereotype.Service;

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
