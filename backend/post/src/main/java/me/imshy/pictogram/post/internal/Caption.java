package me.imshy.pictogram.post.internal;

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
