package me.imshy.pictogram.identity.internal.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RefreshCookieTest {

    @Test
    void issuedCookieIsHttpOnlyStrictAndScopedToTheAuthPath() {
        String header = RefreshCookie.issue("refresh-token-abc", Duration.ofDays(30), true).toString();

        assertThat(header).startsWith("pictogram_refresh=refresh-token-abc").contains("HttpOnly").contains("Secure")
            .contains("SameSite=Strict").contains("Path=/api/auth")
            .contains("Max-Age=" + Duration.ofDays(30).toSeconds());
    }

    @Test
    void theSecureAttributeFollowsTheConfiguredFlag() {
        assertThat(RefreshCookie.issue("t", Duration.ofDays(1), false).toString()).doesNotContain("Secure")
            .contains("SameSite=Strict");
    }

    @Test
    void expiredCookieClearsTheValueButKeepsTheAttributes() {
        String header = RefreshCookie.expired(true).toString();

        assertThat(header).startsWith("pictogram_refresh=;").contains("Max-Age=0").contains("SameSite=Strict")
            .contains("Path=/api/auth");
    }
}
