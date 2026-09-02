package me.imshy.pictogram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nimbusds.jose.JOSEObjectType;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import me.imshy.pictogram.identity.internal.IdentityAuthentication;
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@AppOAuthWebIntegrationTest
class GoogleSignInChainErrorWebTest {

    private static final String ISSUER_ID = SharedGoogle.ISSUER_ID;
    private static final String CLIENT_ID = SharedGoogle.CLIENT_ID;

    @LocalServerPort
    int port;

    @MockitoBean
    IdentityAuthentication authentication;

    @Test
    void anUnexpectedFailureInTheOidcChainRendersAsProblemJsonAndLeavesNoAuthenticatedSession() throws Exception {
        when(authentication.authenticate(any())).thenThrow(new IllegalStateException("boom"));
        SharedGoogle.INSTANCE.enqueueCallback(new DefaultOAuth2TokenCallback(
                ISSUER_ID,
                "chain-error-subject",
                JOSEObjectType.JWT.getType(),
                List.of(CLIENT_ID),
                Map.of("email", "boom@example.com", "email_verified", true),
                3600L));
        var cookies = new CookieManager();

        HttpResponse<Void> authRequest = HttpClient.newBuilder()
                .cookieHandler(cookies)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()
                .send(get("/oauth2/authorization/google"), HttpResponse.BodyHandlers.discarding());
        String handshakeSession = cookies.getCookieStore().getCookies().stream()
                .filter(cookie -> "JSESSIONID".equals(cookie.getName()))
                .map(HttpCookie::getValue)
                .findFirst()
                .orElseThrow(() -> new AssertionError("the OIDC handshake set no JSESSIONID"));

        HttpResponse<String> landing = HttpClient.newBuilder()
                .cookieHandler(cookies)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()
                .send(
                        HttpRequest.newBuilder(URI.create(authRequest
                                        .headers()
                                        .firstValue("Location")
                                        .orElseThrow()))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());

        assertThat(landing.statusCode()).isEqualTo(500);
        assertThat(landing.headers().firstValue("Content-Type").orElseThrow()).contains("application/problem+json");
        assertThat(landing.body()).contains("problems/internal-error");

        HttpResponse<Void> withStaleSession = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()
                .send(
                        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/oauth2/probe"))
                                .header("Cookie", "JSESSIONID=" + handshakeSession)
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.discarding());
        assertThat(withStaleSession.statusCode()).isEqualTo(302);
    }

    private HttpRequest get(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
    }
}
