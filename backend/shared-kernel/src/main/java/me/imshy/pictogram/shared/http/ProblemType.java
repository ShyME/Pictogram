package me.imshy.pictogram.shared.http;

import java.net.URI;
import java.util.Objects;

/**
 * A kind of failure: its stable {@code type} URI (a stable identifier clients branch on,
 * not meant to be dereferenced) and its human-readable {@code title}. {@code shared} owns
 * the cross-cutting ones below; a module declares its own constants for its own failures.
 */
public record ProblemType(String slug, String title) {

    public static final String BASE = "https://pictogram.dev/problems/";

    public static final ProblemType UNAUTHORIZED = new ProblemType("unauthorized", "Authentication required");
    public static final ProblemType FORBIDDEN = new ProblemType("forbidden", "Access denied");
    public static final ProblemType INVALID_CURSOR = new ProblemType("invalid-cursor", "Invalid pagination cursor");
    public static final ProblemType OVERSIZED_BATCH = new ProblemType("oversized-batch", "Too many ids in a batch lookup");
    public static final ProblemType INTERNAL_ERROR = new ProblemType("internal-error", "Internal server error");

    public ProblemType {
        Objects.requireNonNull(slug, "slug");
        Objects.requireNonNull(title, "title");
    }

    public URI uri() {
        return URI.create(BASE + slug);
    }
}
