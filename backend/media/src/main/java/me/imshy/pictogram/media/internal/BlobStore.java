package me.imshy.pictogram.media.internal;

interface BlobStore {

    void put(String key, byte[] bytes);

    byte[] get(String key);

    void remove(String key);
}
