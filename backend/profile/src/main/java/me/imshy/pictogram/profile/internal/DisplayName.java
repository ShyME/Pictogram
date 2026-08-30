package me.imshy.pictogram.profile.internal;

/**
 * The free-text name shown on the profile and feed cards (glossary: 1–50 characters, spaces
 * and emoji allowed, not unique, never in a URL). Optional: a blank input means "not set"
 * and is carried as {@code null}. Length is counted in code points so an emoji costs one,
 * not two; line breaks and other control characters are rejected since it renders inline.
 */
record DisplayName(String value) {

    static final int MAX_LENGTH = 50;

    static DisplayName of(String raw) {
        if (raw == null || raw.isBlank()) {
            return new DisplayName(null);
        }
        String trimmed = raw.strip();
        if (trimmed.codePoints().anyMatch(Character::isISOControl)) {
            throw new InvalidProfileDetailsException("A display name may not contain line breaks or control characters.");
        }
        if (trimmed.codePointCount(0, trimmed.length()) > MAX_LENGTH) {
            throw new InvalidProfileDetailsException("A display name may be at most " + MAX_LENGTH + " characters.");
        }
        return new DisplayName(trimmed);
    }
}
