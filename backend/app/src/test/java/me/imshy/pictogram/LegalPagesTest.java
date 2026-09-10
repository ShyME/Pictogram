package me.imshy.pictogram;

import static me.imshy.pictogram.HttpProbe.contentType;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * The privacy policy and terms of service are plain static files under
 * {@code frontend/public/}, served at these exact paths. The Google Cloud OAuth
 * consent screen is configured with these URLs, so a change to the paths — or a
 * regression in static serving vs. SPA forwarding — breaks Google sign-in
 * setup.
 */
@AppWebIntegrationTest
class LegalPagesTest {

    private static final String SPA_SHELL_MARKER = "<div id=\"root\">";

    @LocalServerPort
    int port;

    private HttpProbe http;

    @BeforeEach
    void bindProbe() {
        http = new HttpProbe("http://localhost:" + port);
    }

    @Test
    void privacyPolicyIsServedAsAStaticHtmlPage() {
        HttpResponse<String> response = http.get("/privacy.html");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(contentType(response)).contains("text/html");
        assertThat(response.body()).contains("<h1>Privacy Policy</h1>").doesNotContain(SPA_SHELL_MARKER);
    }

    @Test
    void termsOfServiceIsServedAsAStaticHtmlPage() {
        HttpResponse<String> response = http.get("/terms.html");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(contentType(response)).contains("text/html");
        assertThat(response.body()).contains("<h1>Terms of Service</h1>").doesNotContain(SPA_SHELL_MARKER);
    }

    @Test
    void theLegalStylesheetIsServedAsAStaticFile() {
        HttpResponse<String> response = http.get("/legal.css");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(contentType(response)).contains("css");
    }
}
