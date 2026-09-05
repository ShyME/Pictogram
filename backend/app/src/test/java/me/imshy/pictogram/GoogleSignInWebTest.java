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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@AppOAuthWebIntegrationTest
class GoogleSignInWebTest {

    private static final String ISSUER_ID = SharedGoogle.ISSUER_ID;
    private static final String CLIENT_ID = SharedGoogle.CLIENT_ID;
    private static final String REFRESH_COOKIE = "pictogram_refresh";

    @LocalServerPort
    int port;

    @Autowired
    JwtDecoder resourceServerJwtDecoder;

    @Test
    void signingInWithGoogleYieldsARefreshCookieRedeemableForAnAccessToken() throws Exception {
        SharedGoogle.INSTANCE.enqueueCallback(googleUser("google-subject-web-1", "ada@example.com"));
        var cookies = new CookieManager();

        signInThroughGoogle(cookies);
        assertThat(refreshCookie(cookies)).isPresent();

        HttpResponse<String> refresh = refresh(refreshCookie(cookies).orElseThrow());

        assertThat(refresh.statusCode()).isEqualTo(200);
        String accessToken = readJsonString(refresh.body(), "accessToken");
        assertThat(resourceServerJwtDecoder.decode(accessToken).getSubject()).satisfies(UUID::fromString);
    }

    @Test
    void replayingAConsumedRefreshCookieIsRejected() throws Exception {
        SharedGoogle.INSTANCE.enqueueCallback(googleUser("google-subject-web-2", "grace@example.com"));
        var cookies = new CookieManager();
        signInThroughGoogle(cookies);
        String presented = refreshCookie(cookies).orElseThrow();

        int firstRotation = refresh(presented).statusCode();
        int replay = refresh(presented).statusCode();

        assertThat(firstRotation).isEqualTo(200);
        assertThat(replay).isEqualTo(401);
    }

    @Test
    void twoConcurrentRefreshesOfTheSameCookieKeepTheSessionAlive() throws Exception {
        SharedGoogle.INSTANCE.enqueueCallback(googleUser("google-subject-web-3", "linus@example.com"));
        var cookies = new CookieManager();
        signInThroughGoogle(cookies);
        String presented = refreshCookie(cookies).orElseThrow();
        String csrfToken = mintCsrfToken();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        var barrier = new CyclicBarrier(2);
        Callable<HttpResponse<String>> attempt = () -> {
            barrier.await();
            return refresh(presented, csrfToken);
        };

        List<Future<HttpResponse<String>>> attempts = pool.invokeAll(List.of(attempt, attempt));
        pool.shutdown();

        var responses = attempts.stream().map(GoogleSignInWebTest::await).toList();
        assertThat(responses).map(HttpResponse::statusCode).containsExactlyInAnyOrder(200, 401);

        String rotated = responses.stream().filter(response -> response.statusCode() == 200)
            .flatMap(response -> rotatedRefreshCookieFrom(response).stream()).findFirst().orElseThrow();
        int followUp = refresh(rotated).statusCode();
        assertThat(followUp).isEqualTo(200);
    }

    @Test
    void anUnverifiedGoogleEmailEndsAtTheSignInErrorRouteWithNoSession() throws Exception {
        SharedGoogle.INSTANCE.enqueueCallback(googleUserWithClaims("google-subject-web-unverified",
            Map.of("email", "unverified@example.com", "email_verified", false)));
        var cookies = new CookieManager();

        HttpResponse<Void> landing = browser(cookies).send(
            HttpRequest.newBuilder(uri("/oauth2/authorization/google")).GET().build(),
            HttpResponse.BodyHandlers.discarding());

        assertThat(landing.uri().getPath()).isEqualTo("/login");
        assertThat(landing.uri().getQuery()).isEqualTo("error=email-unverified");
        assertThat(refreshCookie(cookies)).isEmpty();
    }

    @Test
    void aGoogleResponseWithNoEmailClaimEndsAtTheSignInErrorRoute() throws Exception {
        SharedGoogle.INSTANCE
            .enqueueCallback(googleUserWithClaims("google-subject-web-no-email", Map.of("email_verified", true)));
        var cookies = new CookieManager();

        HttpResponse<Void> landing = browser(cookies).send(
            HttpRequest.newBuilder(uri("/oauth2/authorization/google")).GET().build(),
            HttpResponse.BodyHandlers.discarding());

        assertThat(landing.uri().getPath()).isEqualTo("/login");
        assertThat(landing.uri().getQuery()).isEqualTo("error=email-missing");
        assertThat(refreshCookie(cookies)).isEmpty();
    }

    @Test
    void aFailedGoogleHandshakeEndsAtTheSignInErrorRoute() throws Exception {
        HttpResponse<Void> landing = browser(new CookieManager()).send(HttpRequest
            .newBuilder(uri("/login/oauth2/code/google?error=access_denied&state=nonexistent")).GET().build(),
            HttpResponse.BodyHandlers.discarding());

        assertThat(landing.uri().getPath()).isEqualTo("/login");
        assertThat(landing.uri().getQuery()).isEqualTo("error=sign-in-failed");
    }

    @Test
    void aSuccessfulSignInInvalidatesTheHandshakeServletSession() throws Exception {
        SharedGoogle.INSTANCE.enqueueCallback(googleUser("google-subject-web-session", "hedy@example.com"));
        var cookies = new CookieManager();

        String handshakeSession = signInCapturingHandshakeSession(cookies);
        assertThat(refreshCookie(cookies)).isPresent();

        HttpResponse<Void> withStaleSession = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()
            .send(HttpRequest.newBuilder(uri("/oauth2/probe")).header("Cookie", "JSESSIONID=" + handshakeSession).GET()
                .build(), HttpResponse.BodyHandlers.discarding());

        assertThat(withStaleSession.statusCode()).isEqualTo(302);
    }

    private String signInCapturingHandshakeSession(CookieManager cookies) throws Exception {
        HttpResponse<Void> authRequest = HttpClient.newBuilder().cookieHandler(cookies)
            .followRedirects(HttpClient.Redirect.NEVER).build()
            .send(HttpRequest.newBuilder(uri("/oauth2/authorization/google")).GET().build(),
                HttpResponse.BodyHandlers.discarding());
        String jsessionid = cookies.getCookieStore().getCookies().stream()
            .filter(cookie -> "JSESSIONID".equals(cookie.getName())).map(HttpCookie::getValue).findFirst()
            .orElseThrow(() -> new AssertionError("the OIDC handshake set no JSESSIONID"));
        browser(cookies).send(HttpRequest
            .newBuilder(URI.create(authRequest.headers().firstValue("Location").orElseThrow())).GET().build(),
            HttpResponse.BodyHandlers.discarding());
        return jsessionid;
    }

    private static <T> HttpResponse<T> await(Future<HttpResponse<T>> future) {
        try {
            return future.get();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Optional<String> rotatedRefreshCookieFrom(HttpResponse<?> response) {
        return setCookieValue(response, REFRESH_COOKIE);
    }

    private static Optional<String> setCookieValue(HttpResponse<?> response, String name) {
        String prefix = name + "=";
        return response.headers().allValues("Set-Cookie").stream().filter(header -> header.startsWith(prefix))
            .map(header -> {
                int end = header.indexOf(';');
                return header.substring(prefix.length(), end < 0 ? header.length() : end);
            }).filter(value -> !value.isEmpty()).findFirst();
    }

    private void signInThroughGoogle(CookieManager cookies) throws Exception {
        browser(cookies).send(HttpRequest.newBuilder(uri("/oauth2/authorization/google")).GET().build(),
            HttpResponse.BodyHandlers.discarding());
        assertThat(refreshCookie(cookies)).withFailMessage("Google sign-in set no %s cookie", REFRESH_COOKIE)
            .isPresent();
    }

    private static DefaultOAuth2TokenCallback googleUser(String subject, String email) {
        return googleUserWithClaims(subject, Map.of("email", email, "email_verified", true));
    }

    private static DefaultOAuth2TokenCallback googleUserWithClaims(String subject, Map<String, Object> claims) {
        return new DefaultOAuth2TokenCallback(ISSUER_ID, subject, JOSEObjectType.JWT.getType(), List.of(CLIENT_ID),
            claims, 3600L);
    }

    private HttpClient browser(CookieManager cookies) {
        return HttpClient.newBuilder().cookieHandler(cookies).followRedirects(HttpClient.Redirect.NORMAL).build();
    }

    /**
     * POST /api/auth/refresh carrying the double-submit CSRF token (#125). The
     * identity chain hands the token out by rejecting a tokenless POST with 403 and
     * a fresh {@code XSRF-TOKEN} cookie; a real client — and this helper — echoes
     * that value back in the {@code X-XSRF-TOKEN} header.
     */
    private HttpResponse<String> refresh(String refreshCookieValue) throws Exception {
        return refresh(refreshCookieValue, mintCsrfToken());
    }

    private HttpResponse<String> refresh(String refreshCookieValue, String csrfToken) throws Exception {
        return HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(uri("/api/auth/refresh"))
                .header("Cookie", REFRESH_COOKIE + "=" + refreshCookieValue + "; XSRF-TOKEN=" + csrfToken)
                .header("X-XSRF-TOKEN", csrfToken).POST(HttpRequest.BodyPublishers.noBody()).build(),
            HttpResponse.BodyHandlers.ofString());
    }

    private String mintCsrfToken() throws Exception {
        HttpResponse<Void> seeded = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(uri("/api/auth/refresh")).POST(HttpRequest.BodyPublishers.noBody()).build(),
            HttpResponse.BodyHandlers.discarding());
        assertThat(seeded.statusCode()).isEqualTo(403);
        return setCookieValue(seeded, "XSRF-TOKEN")
            .orElseThrow(() -> new AssertionError("the CSRF filter seeded no XSRF-TOKEN cookie"));
    }

    private static java.util.Optional<String> refreshCookie(CookieManager cookies) {
        return cookies.getCookieStore().getCookies().stream().filter(cookie -> REFRESH_COOKIE.equals(cookie.getName()))
            .map(HttpCookie::getValue).findFirst();
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
