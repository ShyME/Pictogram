package me.imshy.pictogram.media.internal;

/**
 * The object store the image bytes live in — MinIO locally, any S3-compatible store in
 * prod. Keyed by the conventional {@link StorageKeys} strings. The seam that keeps
 * {@link MediaLibrary} free of the S3 client. Every object is the one canonical JPEG
 * (ADR-0006), so the content type is the store's concern, not the caller's.
 */
interface BlobStore {

    void put(String key, byte[] bytes);

    byte[] get(String key);
}
