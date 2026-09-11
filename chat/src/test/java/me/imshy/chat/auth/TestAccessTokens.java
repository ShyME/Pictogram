package me.imshy.chat.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import me.imshy.chat.UserId;

/**
 * Signs Pictogram-shaped access tokens with a self-generated key pair, standing
 * in for `:identity`'s real signing key (chat's own Gradle build has no
 * dependency on it).
 */
public final class TestAccessTokens {

    public static final String ISSUER = "pictogram";

    private final ECKey pair;

    public TestAccessTokens() {
        try {
            pair = new ECKeyGenerator(Curve.P_256).keyID(UUID.randomUUID().toString()).generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    public String publicJwkJson() {
        return pair.toPublicJWK().toJSONString();
    }

    public String issue(UserId subject, Clock clock) {
        Instant now = clock.instant();
        return issue(subject, ISSUER, now, now.plus(Duration.ofMinutes(15)), pair);
    }

    public String issueSignedByAnotherKey(UserId subject, Clock clock) {
        ECKey foreign;
        try {
            foreign = new ECKeyGenerator(Curve.P_256).keyID(UUID.randomUUID().toString()).generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
        Instant now = clock.instant();
        return issue(subject, ISSUER, now, now.plus(Duration.ofMinutes(15)), foreign);
    }

    public String issueExpired(UserId subject, Clock clock) {
        Instant now = clock.instant();
        return issue(subject, ISSUER, now.minus(Duration.ofMinutes(30)), now.minus(Duration.ofMinutes(15)), pair);
    }

    // A token still valid at issuance (JwtTimestampValidator's default clock skew tolerance
    // covers a few seconds either side of `exp`) but expiring almost immediately — for pinning
    // ChatWebSocketHandler's close-at-expiry behaviour (#192) without waiting out a real 15m
    // token.
    public String issueExpiringIn(UserId subject, Clock clock, Duration ttl) {
        Instant now = clock.instant();
        return issue(subject, ISSUER, now, now.plus(ttl), pair);
    }

    public String issueWithIssuer(UserId subject, String issuer, Clock clock) {
        Instant now = clock.instant();
        return issue(subject, issuer, now, now.plus(Duration.ofMinutes(15)), pair);
    }

    // AccessTokenVerifier rejects this outright (#192) — ChatWebSocketHandler needs a real
    // expiry to schedule its close, so a token missing the claim entirely must never resolve.
    public String issueWithoutExpiry(UserId subject, Clock clock) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder().issuer(ISSUER).subject(subject.toString())
            .issueTime(Date.from(clock.instant())).build();
        return sign(claims, pair);
    }

    private String issue(UserId subject, String issuer, Instant issuedAt, Instant expiresAt, ECKey signingKey) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder().issuer(issuer).subject(subject.toString())
            .issueTime(Date.from(issuedAt)).expirationTime(Date.from(expiresAt)).build();
        return sign(claims, signingKey);
    }

    private String sign(JWTClaimsSet claims, ECKey signingKey) {
        try {
            SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(signingKey.getKeyID()).build(), claims);
            jwt.sign(new ECDSASigner(signingKey));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
