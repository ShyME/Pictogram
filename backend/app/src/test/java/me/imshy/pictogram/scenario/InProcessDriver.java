package me.imshy.pictogram.scenario;

import com.nimbusds.jose.JOSEObjectType;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * The in-process {@link PictogramApi} transport: drives the running {@code @SpringBootTest}
 * over real HTTP, standing {@code mock-oauth2-server} in for Google (ADR-0004). Sign-in
 * follows the browser's redirect dance to land a refresh cookie, then redeems it for the
 * access token every later call carries as a bearer token.
 */
public final class InProcessDriver implements PictogramApi {

    static final String ISSUER_ID = "google";
    static final String CLIENT_ID = "pictogram-test";
    static final String CLIENT_SECRET = "pictogram-test-secret";

    private final URI baseUri;
    private final MockOAuth2Server google;
    private final ObjectMapper json;

    public InProcessDriver(URI baseUri, MockOAuth2Server google, ObjectMapper json) {
        this.baseUri = baseUri;
        this.google = google;
        this.json = json;
    }

    @Override
    public Actor registerViaGoogle(String email) {
        google.enqueueCallback(new DefaultOAuth2TokenCallback(
                ISSUER_ID, UUID.randomUUID().toString(), JOSEObjectType.JWT.getType(),
                List.of(CLIENT_ID), Map.of("email", email, "email_verified", true), 3600L));

        var cookies = new CookieManager();
        HttpClient browser = HttpClient.newBuilder()
                .cookieHandler(cookies)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        // The chain ends at the post-login redirect (the SPA, absent here, so a 404) — what
        // matters is the refresh cookie set along the way.
        send(browser, HttpRequest.newBuilder(uri("/oauth2/authorization/google")).GET().build());

        HttpResponse<String> redeemed = send(browser,
                HttpRequest.newBuilder(uri("/api/auth/refresh")).POST(BodyPublishers.noBody()).build());
        require(redeemed, 200, "redeem refresh cookie");
        return new HttpActor(field(redeemed.body(), "accessToken"));
    }

    private final class HttpActor implements Actor {

        private final String accessToken;
        private final HttpClient http = HttpClient.newHttpClient();

        private HttpActor(String accessToken) {
            this.accessToken = accessToken;
        }

        @Override
        public Optional<Profile> currentProfile() {
            HttpResponse<String> response = call("GET", "/api/profiles/me", null);
            if (response.statusCode() == 404) {
                return Optional.empty();
            }
            require(response, 200, "read own profile");
            return Optional.of(profile(response.body()));
        }

        @Override
        public Profile completeOnboarding(String username) {
            return completeOnboarding(username, null, null);
        }

        @Override
        public Profile completeOnboarding(String username, String displayName, String bio) {
            String body = json.writeValueAsString(Map.of(
                    "username", username,
                    "displayName", Optional.ofNullable(displayName).orElse(""),
                    "bio", Optional.ofNullable(bio).orElse("")));
            HttpResponse<String> response = call("POST", "/api/profiles", body);
            require(response, 201, "complete onboarding");
            return profile(response.body());
        }

        @Override
        public Optional<Profile> viewProfile(String username) {
            HttpResponse<String> response = call("GET", "/api/profiles/" + username, null);
            if (response.statusCode() == 404) {
                return Optional.empty();
            }
            require(response, 200, "view a profile by username");
            return Optional.of(profile(response.body()));
        }

        @Override
        public FeedPage openFeed() {
            HttpResponse<String> response = call("GET", "/api/feed", null);
            require(response, 200, "open feed");
            JsonNode page = json.readTree(response.body());
            List<Object> items = new ArrayList<>();
            page.path("items").forEach(items::add);
            JsonNode cursor = page.path("nextCursor");
            return new FeedPage(items, cursor.isNull() || cursor.isMissingNode() ? null : cursor.asString());
        }

        private HttpResponse<String> call(String method, String path, String body) {
            var request = HttpRequest.newBuilder(uri(path))
                    .header("Authorization", "Bearer " + accessToken)
                    .method(method, body == null ? BodyPublishers.noBody() : BodyPublishers.ofString(body));
            if (body != null) {
                request.header("Content-Type", "application/json");
            }
            return send(http, request.build());
        }
    }

    private Profile profile(String body) {
        JsonNode node = json.readTree(body);
        return new Profile(
                node.path("userId").asString(),
                node.path("username").asString(),
                textOrNull(node, "displayName"),
                textOrNull(node, "bio"));
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNull() || value.isMissingNode() ? null : value.asString();
    }

    private String field(String body, String name) {
        return json.readTree(body).path(name).asString();
    }

    private URI uri(String path) {
        return baseUri.resolve(path);
    }

    private static HttpResponse<String> send(HttpClient client, HttpRequest request) {
        try {
            return client.send(request, BodyHandlers.ofString());
        } catch (Exception e) {
            throw new IllegalStateException("HTTP call failed: " + request.method() + " " + request.uri(), e);
        }
    }

    private static void require(HttpResponse<String> response, int expectedStatus, String action) {
        if (response.statusCode() != expectedStatus) {
            throw new AssertionError("Expected %d to %s but got %d: %s"
                    .formatted(expectedStatus, action, response.statusCode(), response.body()));
        }
    }
}
