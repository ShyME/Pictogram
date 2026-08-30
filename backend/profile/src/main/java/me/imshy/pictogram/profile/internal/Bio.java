package me.imshy.pictogram.profile.internal;

/**
 * The optional free-text description on a profile (glossary: up to 160 characters). A blank
 * input means "not set" and is carried as {@code null}. Length is counted in code points,
 * so an emoji costs one; line breaks are allowed.
 */
record Bio(String value) {

    static final int MAX_LENGTH = 160;

    static Bio of(String raw) {
        if (raw == null || raw.isBlank()) {
            return new Bio(null);
        }
        String trimmed = raw.strip();
        if (trimmed.codePointCount(0, trimmed.length()) > MAX_LENGTH) {
            throw new InvalidProfileDetailsException("A bio may be at most " + MAX_LENGTH + " characters.");
        }
        return new Bio(trimmed);
    }
}
