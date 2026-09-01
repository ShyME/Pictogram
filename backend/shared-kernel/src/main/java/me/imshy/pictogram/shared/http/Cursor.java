package me.imshy.pictogram.shared.http;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

public record Cursor(Instant at, UUID id) {

    private static final char SEPARATOR = '|';

    public Cursor {
        Objects.requireNonNull(at, "at");
        Objects.requireNonNull(id, "id");
    }

    public String encode() {
        var raw = at.toString() + SEPARATOR + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static Cursor decode(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidCursorException();
        }
        String raw;
        try {
            raw = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException malformedBase64) {
            throw new InvalidCursorException();
        }
        int separator = raw.lastIndexOf(SEPARATOR);
        if (separator < 0) {
            throw new InvalidCursorException();
        }
        try {
            var at = Instant.parse(raw.substring(0, separator));
            var id = UUID.fromString(raw.substring(separator + 1));
            return new Cursor(at, id);
        } catch (DateTimeParseException | IllegalArgumentException malformedComponent) {
            throw new InvalidCursorException();
        }
    }
}
