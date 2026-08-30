package me.imshy.pictogram;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JOSEObjectType;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.imshy.pictogram.testsupport.SharedPostgres;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The Google OIDC sign-in wired end to end: {@code mock-oauth2-server} stands in for Google
 * (ADR-0004), the browser's redirect dance is driven by a redirect-following HTTP client,
 * and the resulting refresh cookie is exchanged for an access token the resource server
 * accepts. Replaying the spent cookie ends the session.
 */
@SpringBootTest(classes = PictogramApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GoogleSignInWebTest {

    private static final String ISSUER_ID = "google";
    private static final String CLIENT_ID = "pictogram-test";
    private static final String REFRESH_COOKIE = "pictogram_refresh";
    private static final MockOAuth2Server GOOGLE = new MockOAuth2Server();

    static {
        GOOGLE.start();
    }

    @LocalServerPort
    int port;

    @Autowired
    JwtDecoder resourceServerJwtDecoder;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SharedPostgres.registerTo(registry);
        registry.add("spring.security.oauth2.client.registration.google.client-id", () -> CLIENT_ID);
        registry.add("spring.security.oauth2.client.registration.google.client-secret", () -> "pictogram-test-secret");
        registry.add("spring.security.oauth2.client.registration.google.scope", () -> "openid,email");
        registry.add("spring.security.oauth2.client.provider.google.issuer-uri",
                () -> GOOGLE.issuerUrl(ISSUER_ID).toString());
        registry.add("pictogram.auth.cookie-secure", () -> false);
    }

    @AfterAll
    static void stopGoogle() {
        GOOGLE.shutdown();
    }

    @Test
    void signingInWithGoogleYieldsARefreshCookieRedeemableForAnAccessToken() throws Exception {
        GOOGLE.enqueueCallback(googleUser("google-subject-web-1", "ada@example.com"));
        var cookies = new CookieManager();

        signInThroughGoogle(cookies);
        assertThat(refreshCookie(cookies)).isPresent();

        HttpResponse<String> refresh = browser(cookies)
                .send(post("/api/auth/refresh"), HttpResponse.BodyHandlers.ofString());

        assertThat(refresh.statusCode()).isEqualTo(200);
        String accessToken = readJsonString(refresh.body(), "accessToken");
        assertThat(resourceServerJwtDecoder.decode(accessToken).getSubject())
                .satisfies(UUID::fromString);
    }

    @Test
    void replayingAConsumedRefreshCookieEndsTheSession() throws Exception {
        GOOGLE.enqueueCallback(googleUser("google-subject-web-2", "grace@example.com"));
        var cookies = new CookieManager();
        signInThroughGoogle(cookies);
        String stolen = refreshCookie(cookies).orElseThrow();

        int firstRotation = browser(cookies)
                .send(post("/api/auth/refresh"), HttpResponse.BodyHandlers.discarding()).statusCode();
        HttpResponse<Void> replay = browser(new CookieManager())
                .send(postWithRefreshCookie(stolen), HttpResponse.BodyHandlers.discarding());

        assertThat(firstRotation).isEqualTo(200);
        assertThat(replay.statusCode()).isEqualTo(401);
    }

    private void signInThroughGoogle(CookieManager cookies) throws Exception {
        // The redirect chain ends at the post-login page (the SPA, absent in this test, so a
        // 404) — what matters is the refresh cookie the callback set along the way.
        browser(cookies).send(
                HttpRequest.newBuilder(uri("/oauth2/authorization/google")).GET().build(),
                HttpResponse.BodyHandlers.discarding());
        assertThat(refreshCookie(cookies))
                .withFailMessage("Google sign-in set no %s cookie", REFRESH_COOKIE)
                .isPresent();
    }

    private static DefaultOAuth2TokenCallback googleUser(String subject, String email) {
        return new DefaultOAuth2TokenCallback(ISSUER_ID, subject, JOSEObjectType.JWT.getType(),
                List.of(CLIENT_ID), Map.of("email", email, "email_verified", true), 3600L);
    }

    private HttpClient browser(CookieManager cookies) {
        return HttpClient.newBuilder()
                .cookieHandler(cookies)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    private HttpRequest post(String path) {
        return HttpRequest.newBuilder(uri(path)).POST(HttpRequest.BodyPublishers.noBody()).build();
    }

    private HttpRequest postWithRefreshCookie(String value) {
        return HttpRequest.newBuilder(uri("/api/auth/refresh"))
                .header("Cookie", REFRESH_COOKIE + "=" + value)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
    }

    private static java.util.Optional<String> refreshCookie(CookieManager cookies) {
        return cookies.getCookieStore().getCookies().stream()
                .filter(cookie -> REFRESH_COOKIE.equals(cookie.getName()))
                .map(HttpCookie::getValue)
                .findFirst();
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private static String readJsonString(String json, String field) {
        String marker = "\"" + field + "\":\"";
        int start = json.indexOf(marker) + marker.length();
        return json.substring(start, json.indexOf('"', start));
    }
}
