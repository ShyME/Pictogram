package me.imshy.pictogram.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;
import java.util.UUID;

/**
 * The user on whose behalf the current request acts, wrapped so {@code follow} and
 * {@code engagement} cannot confuse it with the user being followed or the post's author
 * (spec §API). Same UUID as that user's {@link UserId}, a distinct type.
 */
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
