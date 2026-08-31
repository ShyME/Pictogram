package me.imshy.pictogram.media.internal;

import me.imshy.pictogram.shared.MediaId;

/**
 * Object-storage keys derived from a {@link MediaId} by convention, so the {@code media}
 * row never has to store them (media/CONTEXT.md). One "folder" per media holds both
 * renditions.
 */
final class StorageKeys {

    private StorageKeys() {
    }

    static String original(MediaId mediaId) {
        return mediaId.value() + "/original.jpg";
    }

    static String thumbnail(MediaId mediaId) {
        return mediaId.value() + "/thumbnail.jpg";
    }
}
