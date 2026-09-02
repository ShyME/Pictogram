package me.imshy.pictogram.scenario;

import com.nimbusds.jose.JOSEObjectType;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.imshy.pictogram.SharedGoogle;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback;
import tools.jackson.databind.ObjectMapper;

public final class InProcessDriver implements PictogramApi {

    private final HttpPictogramApi api;

    public InProcessDriver(URI baseUri, MockOAuth2Server google, ObjectMapper json) {
        this.api = new HttpPictogramApi(baseUri, json, new MockOAuth2SignIn(baseUri, google));
    }

    @Override
    public Actor registerViaGoogle(String email) {
        return api.registerViaGoogle(email);
    }

    private static final class MockOAuth2SignIn implements SignIn {

        private final URI baseUri;
        private final MockOAuth2Server google;

        private MockOAuth2SignIn(URI baseUri, MockOAuth2Server google) {
            this.baseUri = baseUri;
            this.google = google;
        }

        @Override
        public String authenticate(String email) {
            google.enqueueCallback(new DefaultOAuth2TokenCallback(
                    SharedGoogle.ISSUER_ID,
                    UUID.randomUUID().toString(),
                    JOSEObjectType.JWT.getType(),
                    List.of(SharedGoogle.CLIENT_ID),
                    Map.of("email", email, "email_verified", true),
                    3600L));

            var cookies = new CookieManager();
            HttpClient browser = HttpClient.newBuilder()
                    .cookieHandler(cookies)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            try {
                browser.send(
                        HttpRequest.newBuilder(baseUri.resolve("/oauth2/authorization/google"))
                                .GET()
                                .build(),
                        BodyHandlers.discarding());
            } catch (Exception e) {
                throw new IllegalStateException("Google sign-in redirect dance failed", e);
            }

            return ScenarioHttp.refreshCookie(cookies)
                    .orElseThrow(() -> new AssertionError(
                            "No " + ScenarioHttp.REFRESH_COOKIE + " cookie after the Google redirect dance"));
        }
    }
}
