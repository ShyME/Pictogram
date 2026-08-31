package me.imshy.pictogram;

import static me.imshy.pictogram.HttpProbe.contentType;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Black-box guard (ADR-0007) that the deployed image serves the SPA on a client-side deep
 * link: a browser landing straight on {@code /onboarding} — a pasted link, a hard reload —
 * gets the SPA shell back, not {@code /error} (#34). Runs against the running container
 * stack ({@code -PincludeBlackbox}, {@code main} only), reached at {@code localhost:8080}
 * or the {@code PICTOGRAM_BASE_URL} override the Playwright journeys also honour.
 */
@Tag("blackbox")
class SpaDeepLinkBlackboxTest {

    private final HttpProbe http = new HttpProbe(
            Optional.ofNullable(System.getenv("PICTOGRAM_BASE_URL")).orElse("http://localhost:8080"));

    @Test
    void aDirectGetOnAClientRouteReturnsTheSpaShellAsHtml() {
        HttpResponse<String> response = http.get("/onboarding");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(contentType(response)).contains("text/html");
        assertThat(response.body()).contains("id=\"root\"");
    }

    @Test
    void aDirectGetOnANestedProfileRouteReturnsTheSpaShellAsHtml() {
        HttpResponse<String> response = http.get("/u/ada_lovelace");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(contentType(response)).contains("text/html");
        assertThat(response.body()).contains("id=\"root\"");
    }

    @Test
    void unmatchedApiRoutesStayJsonNotTheSpaShell() {
        HttpResponse<String> response = http.get("/api/does-not-exist");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(contentType(response)).contains("application/problem+json");
    }
}
