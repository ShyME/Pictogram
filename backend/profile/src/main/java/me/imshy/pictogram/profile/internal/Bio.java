package me.imshy.pictogram.profile.internal;

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
