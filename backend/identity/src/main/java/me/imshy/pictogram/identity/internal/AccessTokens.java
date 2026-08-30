package me.imshy.pictogram.identity.internal;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import me.imshy.pictogram.identity.InvalidAccessTokenException;
import me.imshy.pictogram.shared.UserId;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;

/**
 * Mints and verifies Pictogram access tokens: an ES256-signed JWT whose subject is the
 * {@link UserId}, valid for {@link #ttl()} from issue (ADR-0004). Signing uses identity's
 * private key; verification uses the public key, so a service extracted from the monolith
 * can verify without holding signing material.
 */
class AccessTokens {

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Clock clock;
    private final Duration ttl;
    private final String issuer;

    AccessTokens(JwtEncoder encoder, JwtDecoder decoder, Clock clock, Duration ttl, String issuer) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.clock = clock;
        this.ttl = ttl;
        this.issuer = issuer;
    }

    String issue(UserId user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.value().toString())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.ES256).build(), claims)).getTokenValue();
    }

    UserId resolve(String accessToken) {
        String subject;
        try {
            subject = decoder.decode(accessToken).getSubject();
        } catch (JwtException invalid) {
            throw new InvalidAccessTokenException("Not a valid Pictogram access token", invalid);
        }
        try {
            return new UserId(UUID.fromString(subject));
        } catch (IllegalArgumentException | NullPointerException notAUserId) {
            throw new InvalidAccessTokenException("Access token subject is not a Pictogram user id", notAUserId);
        }
    }

    Duration ttl() {
        return ttl;
    }
}
