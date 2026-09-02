package me.imshy.pictogram;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

@AppWebIntegrationTest
class SecurityHeadersTest {

    @LocalServerPort
    int port;

    private HttpProbe http;

    @BeforeEach
    void bindProbe() {
        http = new HttpProbe("http://localhost:" + port);
    }

    @Test
    void theSpaShellCarriesTheContentSecurityAndPrivacyHeaders() {
        assertHardeningHeaders(http.get("/"));
    }

    @Test
    void staticAssetsCarryTheSameHeaders() {
        assertHardeningHeaders(http.get("/assets/probe.js"));
    }

    @Test
    void apiResponsesCarryTheSameHeaders() {
        assertHardeningHeaders(http.exchange(http.to("/api/profiles/does-not-exist")
                .header("Accept", "application/json")
                .GET()));
    }

    private static void assertHardeningHeaders(HttpResponse<?> response) {
        var headers = response.headers();

        assertThat(headers.firstValue("Content-Security-Policy"))
                .hasValueSatisfying(csp -> assertThat(csp)
                        .contains("default-src 'self'")
                        .contains("script-src 'self'")
                        .contains("style-src 'self'")
                        .doesNotContain("'unsafe-inline'")
                        .contains("object-src 'none'")
                        .contains("frame-ancestors 'none'"));
        assertThat(headers.firstValue("Referrer-Policy")).hasValue("strict-origin-when-cross-origin");
        assertThat(headers.firstValue("Permissions-Policy"))
                .hasValueSatisfying(policy -> assertThat(policy).contains("geolocation=()"));
        assertThat(headers.firstValue("X-Content-Type-Options")).hasValue("nosniff");
        assertThat(headers.firstValue("X-Frame-Options")).hasValue("DENY");
    }
}
