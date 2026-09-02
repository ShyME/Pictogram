package me.imshy.pictogram;

import static me.imshy.pictogram.HttpProbe.contentType;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import me.imshy.pictogram.shared.http.ProblemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

@AppWebIntegrationTest
class SpaDeepLinkTest {

    private static final String SPA_SHELL_MARKER = "<div id=\"root\">";

    @LocalServerPort
    int port;

    private HttpProbe http;

    @BeforeEach
    void bindProbe() {
        http = new HttpProbe("http://localhost:" + port);
    }

    @Test
    void aDirectGetOnLoginServesTheSpaShell() {
        HttpResponse<String> response = http.get("/login");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(contentType(response)).contains("text/html");
        assertThat(response.body()).contains(SPA_SHELL_MARKER);
    }

    @Test
    void aDirectGetOnOnboardingServesTheSpaShell() {
        HttpResponse<String> response = http.get("/onboarding");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(contentType(response)).contains("text/html");
        assertThat(response.body()).contains(SPA_SHELL_MARKER);
    }

    @Test
    void aDirectGetOnAProfilePageServesTheSpaShell() {
        HttpResponse<String> response = http.get("/u/ada_lovelace");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(contentType(response)).contains("text/html");
        assertThat(response.body()).contains(SPA_SHELL_MARKER);
    }

    @Test
    void aDirectGetOnAProfileFollowerListServesTheSpaShell() {
        HttpResponse<String> response = http.get("/u/ada_lovelace/followers");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(contentType(response)).contains("text/html");
        assertThat(response.body()).contains(SPA_SHELL_MARKER);
    }

    @Test
    void aDirectGetOnAProfileFollowingListServesTheSpaShell() {
        HttpResponse<String> response = http.get("/u/ada_lovelace/following");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains(SPA_SHELL_MARKER);
    }

    @Test
    void anUnknownNestedProfileRouteIsNotForwarded() {
        HttpResponse<String> response = http.get("/u/ada_lovelace/settings");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).doesNotContain(SPA_SHELL_MARKER);
    }

    @Test
    void anUnknownClientRouteStillServesTheSpaShellForTheRouterToHandle() {
        HttpResponse<String> response = http.get("/not-a-real-route");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains(SPA_SHELL_MARKER);
    }

    @Test
    void theRootPathStillServesTheSpaShell() {
        HttpResponse<String> response = http.get("/");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains(SPA_SHELL_MARKER);
    }

    @Test
    void onlyGetIsForwarded() {
        HttpResponse<String> response = http.exchange(http.to("/login").POST(HttpRequest.BodyPublishers.noBody()));

        assertThat(response.statusCode()).isEqualTo(405);
    }

    @Test
    void staticAssetsAreServedAsFilesNotTheSpaShell() {
        HttpResponse<String> response = http.get("/assets/probe.js");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(contentType(response)).contains("javascript");
        assertThat(response.body()).contains("export const probe");
    }

    @Test
    void unmatchedApiRoutesStayProblemDetailsNotTheSpaShell() {
        HttpResponse<String> response = http.exchange(http.to("/api/does-not-exist")
                .header("Authorization", "Bearer not-a-real-token")
                .header("Accept", "text/html")
                .GET());

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(contentType(response)).contains("application/problem+json");
        assertThat(response.body()).contains(ProblemType.UNAUTHORIZED.uri().toString());
    }

    @Test
    void theOidcCallbackStaysWithTheIdentityFilterChain() {
        HttpResponse<String> response = http.get("/login/oauth2/code/google?code=x&state=y");

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).doesNotContain(SPA_SHELL_MARKER);
    }
}
