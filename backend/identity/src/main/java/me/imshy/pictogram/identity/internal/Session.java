package me.imshy.pictogram.identity.internal;

import java.time.Instant;

public record Session(String accessToken, Instant accessTokenExpiresAt, String refreshToken,
    Instant refreshTokenExpiresAt) {
}
