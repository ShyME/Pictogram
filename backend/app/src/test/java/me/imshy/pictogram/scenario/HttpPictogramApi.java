package me.imshy.pictogram.scenario;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Everything about <em>speaking</em> Pictogram's HTTP API, against an injected base
 * {@link URI}: request building, JSON field mapping, multipart encoding, the per-endpoint
 * outcome translation (204/403/404 …) and the {@code require(...)} assertions. It holds no
 * transport specifics — not how the base URI is discovered, and not how a session is
 * obtained.
 *
 * <p><b>The two-adapter shape.</b> A scenario runs against a {@link PictogramApi} composed
 * from two parts: this deep class, shared by every transport, plus a small {@link SignIn}
 * port for the one thing that genuinely differs — how a session is minted. A transport
 * (e.g. {@link InProcessDriver}) holds one of these and delegates; it does not extend it.
 * {@code InProcessDriver} supplies a random {@code @LocalServerPort} base URI and a
 * {@code SignIn} that drives {@code mock-oauth2-server}; the later {@code ContainerDriver}
 * (issue #20) supplies a compose base URI and a {@code SignIn} for the real Google
 * handshake — a config, not a re-implementation of the 200-odd lines below. The sign-in
 * <em>dance</em> (following redirects with the identity provider) is transport and lives in
 * the adapter; redeeming the resulting refresh cookie via {@code POST /api/auth/refresh} is
 * "speaking the API" and lives here.
 */
class HttpPictogramApi implements PictogramApi {

    // The identity module's refresh cookie (ADR-0004); the SignIn adapter yields its value,
    // this class presents it to the redeem endpoint.
    static final String REFRESH_COOKIE = "pictogram_refresh";

    private final URI baseUri;
    private final ObjectMapper json;
    private final SignIn signIn;

    HttpPictogramApi(URI baseUri, ObjectMapper json, SignIn signIn) {
        this.baseUri = baseUri;
        this.json = json;
        this.signIn = signIn;
    }

    @Override
    public Actor registerViaGoogle(String email) {
        String refreshCookie = signIn.authenticate(email);
        HttpResponse<String> redeemed = send(HttpClient.newHttpClient(), HttpRequest.newBuilder(uri("/api/auth/refresh"))
                .header("Cookie", REFRESH_COOKIE + "=" + refreshCookie)
                .POST(BodyPublishers.noBody())
                .build());
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
            HttpResponse<String> response = call("POST", "/api/profiles", profileBody(username, displayName, bio));
            require(response, 201, "complete onboarding");
            return profile(response.body());
        }

        @Override
        public Profile editProfile(String username, String displayName, String bio) {
            HttpResponse<String> response = call("PUT", "/api/profiles/me", profileBody(username, displayName, bio));
            require(response, 200, "edit own profile");
            return profile(response.body());
        }

        private String profileBody(String username, String displayName, String bio) {
            return json.writeValueAsString(Map.of(
                    "username", username,
                    "displayName", Optional.ofNullable(displayName).orElse(""),
                    "bio", Optional.ofNullable(bio).orElse("")));
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
        public String uploadPhoto(byte[] image) {
            String boundary = "----pictogram" + UUID.randomUUID();
            var request = HttpRequest.newBuilder(uri("/api/media"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(BodyPublishers.ofByteArray(multipartBody(boundary, image)))
                    .build();
            HttpResponse<String> response = send(http, request);
            require(response, 201, "upload a photo");
            return field(response.body(), "mediaId");
        }

        @Override
        public Post publishPost(String mediaId, String caption) {
            String body = json.writeValueAsString(Map.of("mediaId", mediaId, "caption",
                    Optional.ofNullable(caption).orElse("")));
            HttpResponse<String> response = call("POST", "/api/posts", body);
            require(response, 201, "publish a post");
            return post(response.body());
        }

        @Override
        public DeleteOutcome deletePost(String postId) {
            HttpResponse<String> response = call("DELETE", "/api/posts/" + postId, null);
            return switch (response.statusCode()) {
                case 204 -> DeleteOutcome.DELETED;
                case 403 -> DeleteOutcome.FORBIDDEN;
                default -> throw new AssertionError(
                        "Unexpected status deleting a post: " + response.statusCode() + ": " + response.body());
            };
        }

        @Override
        public List<Post> postsOf(String userId) {
            HttpResponse<String> response = call("GET", "/api/posts?author=" + userId, null);
            require(response, 200, "read a post grid");
            List<Post> posts = new ArrayList<>();
            json.readTree(response.body()).path("items").forEach(item -> posts.add(post(item)));
            return posts;
        }

        @Override
        public FollowOutcome follow(String userId) {
            HttpResponse<String> response = call("PUT", "/api/follows/" + userId, null);
            return switch (response.statusCode()) {
                case 204 -> FollowOutcome.OK;
                case 422 -> FollowOutcome.SELF_FOLLOW;
                default -> throw new AssertionError(
                        "Unexpected status following a user: " + response.statusCode() + ": " + response.body());
            };
        }

        @Override
        public void unfollow(String userId) {
            require(call("DELETE", "/api/follows/" + userId, null), 204, "unfollow a user");
        }

        @Override
        public AccountPage followers(String userId, String cursor, Integer limit) {
            return accountPage("/api/follows/" + userId + "/followers", cursor, limit);
        }

        @Override
        public AccountPage following(String userId, String cursor, Integer limit) {
            return accountPage("/api/follows/" + userId + "/following", cursor, limit);
        }

        private AccountPage accountPage(String path, String cursor, Integer limit) {
            var query = new StringBuilder();
            if (cursor != null) {
                query.append(query.isEmpty() ? '?' : '&').append("cursor=").append(cursor);
            }
            if (limit != null) {
                query.append(query.isEmpty() ? '?' : '&').append("limit=").append(limit);
            }
            HttpResponse<String> response = call("GET", path + query, null);
            require(response, 200, "read a follow list");
            JsonNode page = json.readTree(response.body());
            List<String> userIds = new ArrayList<>();
            page.path("items").forEach(id -> userIds.add(id.asString()));
            JsonNode next = page.path("nextCursor");
            return new AccountPage(userIds, next.isNull() || next.isMissingNode() ? null : next.asString());
        }

        @Override
        public FollowRelationship followRelationship(String userId) {
            HttpResponse<String> response = call("GET", "/api/follows/" + userId, null);
            require(response, 200, "read a follow relationship");
            JsonNode node = json.readTree(response.body());
            return new FollowRelationship(
                    node.path("followerCount").asLong(),
                    node.path("followingCount").asLong(),
                    node.path("followedByViewer").asBoolean());
        }

        @Override
        public Map<String, FollowRelationship> followRelationships(String... userIds) {
            // Repeated ids= params, like the frontend and the OpenAPI schema send — not the
            // comma-joined form, so a scenario failure looks like a real client's request.
            String query = Arrays.stream(userIds).map(id -> "ids=" + id).collect(Collectors.joining("&"));
            HttpResponse<String> response = call("GET", "/api/follows?" + query, null);
            require(response, 200, "read a batch of follow relationships");
            Map<String, FollowRelationship> byId = new LinkedHashMap<>();
            json.readTree(response.body()).forEach(node -> byId.put(node.path("userId").asString(),
                    new FollowRelationship(node.path("followerCount").asLong(),
                            node.path("followingCount").asLong(),
                            node.path("followedByViewer").asBoolean())));
            return byId;
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

    private Post post(String body) {
        return post(json.readTree(body));
    }

    private static Post post(JsonNode node) {
        return new Post(
                node.path("postId").asString(),
                node.path("authorId").asString(),
                node.path("mediaId").asString(),
                textOrNull(node, "caption"),
                node.path("publishedAt").asString());
    }

    private static byte[] multipartBody(String boundary, byte[] file) {
        var head = ("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"photo.jpg\"\r\n"
                + "Content-Type: image/jpeg\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        var tail = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        var body = new byte[head.length + file.length + tail.length];
        System.arraycopy(head, 0, body, 0, head.length);
        System.arraycopy(file, 0, body, head.length, file.length);
        System.arraycopy(tail, 0, body, head.length + file.length, tail.length);
        return body;
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
