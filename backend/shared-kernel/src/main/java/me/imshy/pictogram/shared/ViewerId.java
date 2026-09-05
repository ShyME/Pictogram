package me.imshy.pictogram.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;
import java.util.UUID;

public record ViewerId(UUID value) {

    public ViewerId {
        Objects.requireNonNull(value, "ViewerId value must not be null");
    }

    public static ViewerId of(UserId userId) {
        return new ViewerId(userId.value());
    }

    public static ViewerId random() {
        return new ViewerId(UUID.randomUUID());
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ViewerId fromString(String value) {
        return new ViewerId(UUID.fromString(value));
    }

    public UserId asUserId() {
        return new UserId(value);
    }

    @JsonValue
    @Override
    public String toString() {
        return value.toString();
    }
}
