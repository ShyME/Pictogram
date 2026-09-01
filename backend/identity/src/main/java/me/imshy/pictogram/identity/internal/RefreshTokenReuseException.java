package me.imshy.pictogram.identity.internal;

import java.util.UUID;

/**
 * A refresh token was replayed outside the rotation grace — consumed longer than
 * {@code refresh-token-rotation-grace} ago, or belonging to an already-revoked family. Unlike
 * a bare {@link InvalidRefreshTokenException}, this revokes the whole family: the person is
 * back to signing in.
 */
public class RefreshTokenReuseException extends InvalidRefreshTokenException {

    public RefreshTokenReuseException(UUID familyId) {
        super("Refresh token replay detected; family " + familyId + " revoked");
    }
}
