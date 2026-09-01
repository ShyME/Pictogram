package me.imshy.pictogram.identity.internal;

import java.util.UUID;

public class RefreshTokenReuseException extends InvalidRefreshTokenException {

    public RefreshTokenReuseException(UUID familyId) {
        super("Refresh token replay detected; family " + familyId + " revoked");
    }
}
