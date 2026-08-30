package me.imshy.pictogram.identity;

import me.imshy.pictogram.shared.UserId;

/**
 * identity's published interface (spec §identity): given a Pictogram access token, resolve
 * and verify the {@link UserId} it was minted for. Verification is signature + expiry
 * against identity's own key — never a Google token.
 *
 * <p>The {@code :app} resource server verifies tokens through the {@code JwtDecoder} identity
 * contributes; this interface is the same capability for in-process callers and for a
 * service later extracted from the monolith (ADR-0001).
 */
public interface PictogramAccessTokens {

    /**
     * @throws InvalidAccessTokenException if the token is malformed, expired, or not signed
     *                                     by identity's current key
     */
    UserId resolve(String accessToken);
}
