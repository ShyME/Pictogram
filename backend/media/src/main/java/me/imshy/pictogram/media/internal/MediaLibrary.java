package me.imshy.pictogram.media.internal;

import java.time.Clock;
import java.util.Optional;
import me.imshy.pictogram.media.MediaCatalog;
import me.imshy.pictogram.media.internal.ImagePipeline.PhotoRenditions;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.UserId;
import org.springframework.stereotype.Service;

@Service
public class MediaLibrary implements MediaCatalog {

    private final Medias medias;
    private final ImagePipeline imagePipeline;
    private final BlobStore blobStore;
    private final Clock clock;

    MediaLibrary(Medias medias, ImagePipeline imagePipeline, BlobStore blobStore, Clock clock) {
        this.medias = medias;
        this.imagePipeline = imagePipeline;
        this.blobStore = blobStore;
        this.clock = clock;
    }

    public MediaId upload(UserId owner, byte[] upload) {
        PhotoRenditions renditions = imagePipeline.transcode(upload);

        Media media = Media.uploadedBy(owner, clock.instant());
        MediaId mediaId = media.mediaId();
        blobStore.put(StorageKeys.original(mediaId), renditions.original());
        blobStore.put(StorageKeys.thumbnail(mediaId), renditions.thumbnail());
        medias.save(media);
        return mediaId;
    }

    public byte[] original(MediaId mediaId) {
        requireExists(mediaId);
        return blobStore.get(StorageKeys.original(mediaId));
    }

    public byte[] thumbnail(MediaId mediaId) {
        requireExists(mediaId);
        return blobStore.get(StorageKeys.thumbnail(mediaId));
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
