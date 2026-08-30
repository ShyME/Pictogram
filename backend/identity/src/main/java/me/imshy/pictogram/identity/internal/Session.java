package me.imshy.pictogram.identity.internal;

import java.time.Instant;

/**
 * The pair of tokens a caller holds after signing in or refreshing: a short-lived access
 * token for the SPA to send with each request, and the rotating refresh token that lives
 * in an httpOnly cookie (ADR-0004).
 */
public record Session(String accessToken, Instant accessTokenExpiresAt,
        String refreshToken, Instant refreshTokenExpiresAt) {
}
