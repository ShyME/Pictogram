package me.imshy.pictogram.profile.internal;

record DisplayName(String value) {

    static final int MAX_LENGTH = 50;

    static DisplayName of(String raw) {
        if (raw == null || raw.isBlank()) {
            return new DisplayName(null);
        }
        String trimmed = raw.strip();
        if (trimmed.codePoints().anyMatch(Character::isISOControl)) {
            throw new InvalidProfileDetailsException(
                "A display name may not contain line breaks or control characters.");
        }
        if (trimmed.codePointCount(0, trimmed.length()) > MAX_LENGTH) {
            throw new InvalidProfileDetailsException("A display name may be at most " + MAX_LENGTH + " characters.");
        }
        return new DisplayName(trimmed);
    }
}
