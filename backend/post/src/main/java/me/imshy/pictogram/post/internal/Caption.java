package me.imshy.pictogram.post.internal;

/**
 * The optional free text on a post (glossary: up to 2200 characters). A blank input means
 * "no caption" and is carried as {@code null}. Length is counted in code points, so an emoji
 * costs one; line breaks are kept.
 */
record Caption(String value) {

    static final int MAX_LENGTH = 2200;

    static Caption of(String raw) {
        if (raw == null || raw.isBlank()) {
            return new Caption(null);
        }
        String trimmed = raw.strip();
        if (trimmed.codePointCount(0, trimmed.length()) > MAX_LENGTH) {
            throw new CaptionTooLongException();
        }
        return new Caption(trimmed);
    }
}
