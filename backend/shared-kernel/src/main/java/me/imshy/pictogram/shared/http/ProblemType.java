package me.imshy.pictogram.shared.http;

import java.net.URI;
import java.util.Objects;

public record ProblemType(String slug, String title) {

    public static final String BASE = "https://pictogram.dev/problems/";

    public static final ProblemType UNAUTHORIZED = new ProblemType("unauthorized", "Authentication required");
    public static final ProblemType FORBIDDEN = new ProblemType("forbidden", "Access denied");
    public static final ProblemType INVALID_CURSOR = new ProblemType("invalid-cursor", "Invalid pagination cursor");
    public static final ProblemType OVERSIZED_BATCH = new ProblemType("oversized-batch",
        "Too many ids in a batch lookup");
    public static final ProblemType INTERNAL_ERROR = new ProblemType("internal-error", "Internal server error");

    public ProblemType {
        Objects.requireNonNull(slug, "slug");
        Objects.requireNonNull(title, "title");
    }

    public URI uri() {
        return URI.create(BASE + slug);
    }
}
