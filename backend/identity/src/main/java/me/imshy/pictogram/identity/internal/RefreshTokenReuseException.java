package me.imshy.pictogram.identity.internal;

import java.util.UUID;

/**
 * A refresh token that was already consumed (or belongs to a revoked family) was presented
 * again — a replay. The whole family is revoked and the person is back to signing in.
 */
public class RefreshTokenReuseException extends InvalidRefreshTokenException {

    public RefreshTokenReuseException(UUID familyId) {
        super("Refresh token replay detected; family " + familyId + " revoked");
    }
}
