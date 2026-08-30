package me.imshy.pictogram.shared.http;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

/**
 * An opaque keyset cursor marking a position in a list ordered by {@code publishedAt}
 * descending with the id as tiebreaker — the ordering the feed and the profile grid share.
 * Encodes to a Base64URL token the client treats as opaque; {@link #decode} rejects
 * anything it did not produce with {@link InvalidCursorException}.
 */
public record Cursor(Instant publishedAt, UUID id) {

    private static final char SEPARATOR = '|';

    public Cursor {
        Objects.requireNonNull(publishedAt, "publishedAt");
        Objects.requireNonNull(id, "id");
    }

    public String encode() {
        var raw = publishedAt.toString() + SEPARATOR + id;
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
            var publishedAt = Instant.parse(raw.substring(0, separator));
            var id = UUID.fromString(raw.substring(separator + 1));
            return new Cursor(publishedAt, id);
        } catch (DateTimeParseException | IllegalArgumentException malformedComponent) {
            throw new InvalidCursorException();
        }
    }
}
