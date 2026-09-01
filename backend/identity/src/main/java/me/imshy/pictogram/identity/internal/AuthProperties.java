package me.imshy.pictogram.identity.internal;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Tunables for the tokens identity issues.
 *
 * @param accessTokenTtl   how long a minted access token is accepted (ADR-0004: ~15 min)
 * @param refreshTokenTtl  how long a refresh-token family stays usable without a fresh sign-in
 * @param refreshTokenRotationGrace  how soon after a refresh token is consumed a second
 *                          presentation of it is still treated as a benign concurrent refresh
 *                          (401, no revoke) rather than token theft (ADR-0004)
 * @param issuer           the {@code iss} claim, also checked on verification
 * @param signingKey       the private EC JWK (JSON) used to sign; a process-lifetime key is
 *                          generated when blank — acceptable for local and test, not production
 * @param postLoginRedirect where the browser lands after a successful Google sign-in
 * @param signInErrorRedirect where the browser lands when Google sign-in can't complete —
 *                          an unusable Google account, a declined consent, an unexpected
 *                          failure. Carries a generic {@code ?error=} reason the SPA shows a
 *                          message for; a specific failure swaps in its own slug (#27)
 * @param cookieSecure     whether the refresh-token cookie carries {@code Secure}; only turn
 *                          it off for plain-HTTP local development
 */
@ConfigurationProperties("pictogram.auth")
public record AuthProperties(
        @DefaultValue("15m") Duration accessTokenTtl,
        @DefaultValue("30d") Duration refreshTokenTtl,
        @DefaultValue("60s") Duration refreshTokenRotationGrace,
        @DefaultValue("pictogram") String issuer,
        String signingKey,
        @DefaultValue("/") String postLoginRedirect,
        @DefaultValue("/login?error=sign-in-failed") String signInErrorRedirect,
        @DefaultValue("true") boolean cookieSecure) {
}
