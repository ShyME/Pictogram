package me.imshy.pictogram.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
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

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static UserId fromString(String value) {
        return new UserId(UUID.fromString(value));
    }

    @JsonValue
    @Override
    public String toString() {
        return value.toString();
    }
}
