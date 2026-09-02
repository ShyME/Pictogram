package me.imshy.pictogram.scenario;

import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * {@link SignIn} for the container transport: walks the Google sign-in exactly as a browser would,
 * against the {@code mock-oauth2-server} bundled by {@code compose.mock-oauth.yaml}. Hit
 * {@code /oauth2/authorization/google}, follow the redirect to the mock's interactive login form,
 * submit the subject, and let the redirects carry the authorization code back to the app, which
 * plants the {@code pictogram_refresh} cookie. No enqueued callbacks, no shared server handle —
 * this is the real handshake over HTTP.
 */
final class InteractiveLoginSignIn implements SignIn {

    private final URI baseUri;

    InteractiveLoginSignIn(URI baseUri) {
        this.baseUri = baseUri;
    }

    @Override
    public String authenticate(String subject) {
        var cookies = new CookieManager();
        HttpClient browser = HttpClient.newBuilder()
                .cookieHandler(cookies)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpResponse<String> loginForm = ScenarioHttp.send(
                browser,
                HttpRequest.newBuilder(baseUri.resolve("/oauth2/authorization/google"))
                        .GET()
                        .build());

        // A non-interactive mock would have finished the dance on that GET already.
        if (ScenarioHttp.refreshCookie(cookies).isEmpty()) {
            ScenarioHttp.require(loginForm, 200, "reach the mock login form");
            ScenarioHttp.send(
                    browser,
                    HttpRequest.newBuilder(loginForm.uri())
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(
                                    "username=" + URLEncoder.encode(subject, StandardCharsets.UTF_8)))
                            .build());
        }

        return ScenarioHttp.refreshCookie(cookies)
                .orElseThrow(() -> new AssertionError(
                        "No " + ScenarioHttp.REFRESH_COOKIE + " cookie after the Google sign-in handshake"));
    }
}
