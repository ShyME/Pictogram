package me.imshy.chat.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import me.imshy.chat.UserId;
import org.junit.jupiter.api.Test;

class AccessTokenVerifierTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-05T12:00:00Z"), ZoneOffset.UTC);
    private final TestAccessTokens tokens = new TestAccessTokens();
    private final AccessTokenVerifier verifier = new AccessTokenVerifier(
        PublicSigningKey.fromJwkJson(tokens.publicJwkJson()).jwkSource(), TestAccessTokens.ISSUER, clock);

    @Test
    void resolvesTheUserIdOfAValidToken() {
        UserId sender = UserId.random();

        ResolvedAccessToken resolved = verifier.resolve(tokens.issue(sender, clock));

        assertThat(resolved.userId()).isEqualTo(sender);
    }

    @Test
    void resolvesTheExpiryOfAValidToken() {
        ResolvedAccessToken resolved = verifier.resolve(tokens.issue(UserId.random(), clock));

        assertThat(resolved.expiresAt()).isEqualTo(clock.instant().plus(Duration.ofMinutes(15)));
    }

    @Test
    void rejectsATokenSignedByAForeignKey() {
        String tampered = tokens.issueSignedByAnotherKey(UserId.random(), clock);

        assertThatThrownBy(() -> verifier.resolve(tampered)).isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void rejectsAnExpiredToken() {
        String expired = tokens.issueExpired(UserId.random(), clock);

        assertThatThrownBy(() -> verifier.resolve(expired)).isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void rejectsATokenFromAnUnexpectedIssuer() {
        String wrongIssuer = tokens.issueWithIssuer(UserId.random(), "not-pictogram", clock);

        assertThatThrownBy(() -> verifier.resolve(wrongIssuer)).isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void rejectsAMalformedToken() {
        assertThatThrownBy(() -> verifier.resolve("not-a-jwt")).isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void rejectsATokenWithNoExpiryClaimAtAll() {
        String noExpiry = tokens.issueWithoutExpiry(UserId.random(), clock);

        assertThatThrownBy(() -> verifier.resolve(noExpiry)).isInstanceOf(InvalidAccessTokenException.class);
    }
}
