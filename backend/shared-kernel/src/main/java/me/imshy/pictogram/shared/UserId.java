package me.imshy.pictogram.shared;

import java.util.Objects;
import java.util.UUID;

/** A user, referenced across modules by value only — never a cross-schema key (ADR-0002). */
public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "UserId value must not be null");
    }

    public static UserId random() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId fromString(String value) {
        return new UserId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
