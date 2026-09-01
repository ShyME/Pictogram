package me.imshy.pictogram.identity.internal.accesstoken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Duration;
import me.imshy.pictogram.identity.InvalidAccessTokenException;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.testsupport.MutableClock;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class AccessTokensTest {

    private static final Duration TTL = Duration.ofMinutes(15);
    private static final String ISSUER = "pictogram";

    private final MutableClock clock = MutableClock.at("2026-08-31T10:00:00Z");
    private final SigningKey key = SigningKey.generate();
    private final AccessTokens accessTokens = new AccessTokens(
            new NimbusJwtEncoder(key.jwkSource()),
            AccessTokenVerification.decoder(key.jwkSource(), ISSUER, clock),
            clock, TTL, ISSUER);

    @Test
    void resolvesAFreshTokenBackToItsUser() {
        var user = UserId.random();

        assertThat(accessTokens.resolve(accessTokens.issue(user))).isEqualTo(user);
    }

    @Test
    void rejectsATokenOnceItHasExpired() {
        String token = accessTokens.issue(UserId.random());

        clock.advance(TTL.plusMinutes(5));

        assertThatExceptionOfType(InvalidAccessTokenException.class)
                .isThrownBy(() -> accessTokens.resolve(token));
    }

    @Test
    void rejectsATokenSignedByADifferentKey() {
        var otherIssuer = new AccessTokens(
                new NimbusJwtEncoder(SigningKey.generate().jwkSource()),
                AccessTokenVerification.decoder(key.jwkSource(), ISSUER, clock),
                clock, TTL, ISSUER);
        String foreignToken = otherIssuer.issue(UserId.random());

        assertThatExceptionOfType(InvalidAccessTokenException.class)
                .isThrownBy(() -> accessTokens.resolve(foreignToken));
    }

    @Test
    void rejectsAGarbageString() {
        assertThatExceptionOfType(InvalidAccessTokenException.class)
                .isThrownBy(() -> accessTokens.resolve("not.a.jwt"));
    }
}
