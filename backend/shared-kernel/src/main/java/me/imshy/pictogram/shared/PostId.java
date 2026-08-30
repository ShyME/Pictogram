package me.imshy.pictogram.shared;

import java.util.Objects;
import java.util.UUID;

/** A published post, referenced across modules by value only. */
public record PostId(UUID value) {

    public PostId {
        Objects.requireNonNull(value, "PostId value must not be null");
    }

    public static PostId random() {
        return new PostId(UUID.randomUUID());
    }

    public static PostId fromString(String value) {
        return new PostId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
