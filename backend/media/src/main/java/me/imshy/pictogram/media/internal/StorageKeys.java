package me.imshy.pictogram.media.internal;

import me.imshy.pictogram.shared.MediaId;

final class StorageKeys {

    private StorageKeys() {}

    static String original(MediaId mediaId) {
        return mediaId.value() + "/original.jpg";
    }

    static String thumbnail(MediaId mediaId) {
        return mediaId.value() + "/thumbnail.jpg";
    }
}
