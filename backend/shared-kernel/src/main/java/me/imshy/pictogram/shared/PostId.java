package me.imshy.pictogram.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
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

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PostId fromString(String value) {
        return new PostId(UUID.fromString(value));
    }

    @JsonValue
    @Override
    public String toString() {
        return value.toString();
    }
}
