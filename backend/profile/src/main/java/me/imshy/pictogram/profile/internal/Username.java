package me.imshy.pictogram.profile.internal;

import java.util.regex.Pattern;

/**
 * A user's unique, URL-safe handle. The shape rule — 3–20 characters of {@code [a-z0-9_]} —
 * is enforced in the constructor so a malformed handle cannot exist in the domain.
 * Uniqueness is a separate, database-backed concern checked during onboarding.
 */
record Username(String value) {

    private static final Pattern SHAPE = Pattern.compile("^[a-z0-9_]{3,20}$");

    Username {
        if (value == null || !SHAPE.matcher(value).matches()) {
            throw new MalformedUsernameException();
        }
    }
}
