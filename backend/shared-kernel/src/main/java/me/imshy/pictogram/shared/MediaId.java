package me.imshy.pictogram.shared;

import java.util.Objects;
import java.util.UUID;

/** A stored image, referenced across modules by value only. */
public record MediaId(UUID value) {

    public MediaId {
        Objects.requireNonNull(value, "MediaId value must not be null");
    }

    public static MediaId random() {
        return new MediaId(UUID.randomUUID());
    }

    public static MediaId fromString(String value) {
        return new MediaId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
