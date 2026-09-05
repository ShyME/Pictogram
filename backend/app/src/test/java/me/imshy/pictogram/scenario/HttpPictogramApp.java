package me.imshy.pictogram.scenario;

import static me.imshy.pictogram.scenario.HttpHelper.require;
import static me.imshy.pictogram.scenario.HttpHelper.send;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class HttpPictogramApp implements PictogramApp {

    private final URI baseUri;
    private final ObjectMapper json;
    private final SignInStrategy signIn;
    private final UsernameStrategy usernameStrategy;

    HttpPictogramApp(URI baseUri, ObjectMapper json, SignInStrategy signIn, UsernameStrategy usernameStrategy) {
        this.baseUri = baseUri;
        this.json = json;
        this.signIn = signIn;
        this.usernameStrategy = usernameStrategy;
    }

    @Override
    public PictogramApi registerViaGoogle(String email) {
        String refreshCookie = signIn.authenticate(email);
        HttpResponse<String> redeemed = redeem(refreshCookie, null);
        if (redeemed.statusCode() == 403) {
            // #125: the identity chain double-submits a CSRF token. A tokenless POST is
            // rejected
            // with a fresh XSRF-TOKEN cookie; the retry echoes it back, exactly as the SPA
            // does.
            redeemed = redeem(refreshCookie, csrfTokenFrom(redeemed));
        }
        require(redeemed, 200, "redeem refresh cookie");
        return new HttpPictogramApi(field(redeemed.body(), "accessToken"));
    }

    private HttpResponse<String> redeem(String refreshCookie, String csrfToken) {
        String cookie = HttpHelper.REFRESH_COOKIE + "=" + refreshCookie;
        var request = HttpRequest.newBuilder(uri("/api/auth/refresh")).POST(BodyPublishers.noBody());
        if (csrfToken != null) {
            request.header("Cookie", cookie + "; XSRF-TOKEN=" + csrfToken).header("X-XSRF-TOKEN", csrfToken);
        } else {
            request.header("Cookie", cookie);
        }
        return send(HttpClient.newHttpClient(), request.build());
    }

    private static String csrfTokenFrom(HttpResponse<?> response) {
        String prefix = "XSRF-TOKEN=";
        return response.headers().allValues("Set-Cookie").stream().filter(header -> header.startsWith(prefix))
            .map(header -> {
                int end = header.indexOf(';');
                return header.substring(prefix.length(), end < 0 ? header.length() : end);
            }).filter(value -> !value.isEmpty()).findFirst()
            .orElseThrow(() -> new AssertionError("the refresh 403 seeded no XSRF-TOKEN cookie"));
    }

    private final class HttpPictogramApi implements PictogramApi {

        private final String accessToken;
        private final HttpClient http = HttpClient.newHttpClient();

        private HttpPictogramApi(String accessToken) {
            this.accessToken = accessToken;
        }

        @Override
        public String accessToken() {
            return accessToken;
        }

        @Override
        public Optional<Profile> currentProfile() {
            HttpResponse<String> response = call("GET", "/api/profiles/me", null);
            if (response.statusCode() == 404) {
                return Optional.empty();
            }
            require(response, 200, "read own profile");
            return Optional.of(usernameStrategy.strip(profile(response.body())));
        }

        @Override
        public Profile completeOnboarding(String username) {
            return completeOnboarding(username, null, null);
        }

        @Override
        public Profile completeOnboarding(String username, String displayName, String bio) {
            HttpResponse<String> response = call("POST", "/api/profiles",
                profileBody(usernameStrategy.qualify(username), displayName, bio));
            require(response, 201, "complete onboarding");
            return usernameStrategy.strip(profile(response.body()));
        }

        @Override
        public Profile editProfile(String username, String displayName, String bio) {
            HttpResponse<String> response = call("PUT", "/api/profiles/me",
                profileBody(usernameStrategy.qualify(username), displayName, bio));
            require(response, 200, "edit own profile");
            return usernameStrategy.strip(profile(response.body()));
        }

        private String profileBody(String username, String displayName, String bio) {
            return json.writeValueAsString(Map.of("username", username, "displayName",
                Optional.ofNullable(displayName).orElse(""), "bio", Optional.ofNullable(bio).orElse("")));
        }

        @Override
        public Optional<Profile> viewProfile(String username) {
            HttpResponse<String> response = call("GET", "/api/profiles/" + usernameStrategy.qualify(username), null);
            if (response.statusCode() == 404) {
                return Optional.empty();
            }
            require(response, 200, "view a profile by username");
            return Optional.of(usernameStrategy.strip(profile(response.body())));
        }

        @Override
        public String uploadPhoto(byte[] image) {
            String boundary = "----pictogram" + UUID.randomUUID();
            var request = HttpRequest.newBuilder(uri("/api/media")).header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(BodyPublishers.ofByteArray(multipartBody(boundary, image))).build();
            HttpResponse<String> response = send(http, request);
            require(response, 201, "upload a photo");
            return field(response.body(), "mediaId");
        }

        @Override
        public Post publishPost(String mediaId, String caption) {
            String body = json
                .writeValueAsString(Map.of("mediaId", mediaId, "caption", Optional.ofNullable(caption).orElse("")));
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
            return new FollowRelationship(node.path("followerCount").asLong(), node.path("followingCount").asLong(),
                node.path("followedByViewer").asBoolean());
        }

        @Override
        public Map<String, FollowRelationship> followRelationships(String... userIds) {
            String query = Arrays.stream(userIds).map(id -> "ids=" + id).collect(Collectors.joining("&"));
            HttpResponse<String> response = call("GET", "/api/follows?" + query, null);
            require(response, 200, "read a batch of follow relationships");
            Map<String, FollowRelationship> byId = new LinkedHashMap<>();
            json.readTree(response.body())
                .forEach(node -> byId.put(node.path("userId").asString(),
                    new FollowRelationship(node.path("followerCount").asLong(), node.path("followingCount").asLong(),
                        node.path("followedByViewer").asBoolean())));
            return byId;
        }

        @Override
        public void like(String postId) {
            require(call("PUT", "/api/likes/" + postId, null), 204, "like a post");
        }

        @Override
        public void unlike(String postId) {
            require(call("DELETE", "/api/likes/" + postId, null), 204, "unlike a post");
        }

        @Override
        public Map<String, PostLikes> likesOf(String... postIds) {
            String query = Arrays.stream(postIds).map(id -> "postIds=" + id).collect(Collectors.joining("&"));
            HttpResponse<String> response = call("GET", "/api/likes?" + query, null);
            require(response, 200, "read a batch of post likes");
            Map<String, PostLikes> byId = new LinkedHashMap<>();
            json.readTree(response.body()).forEach(node -> byId.put(node.path("postId").asString(),
                new PostLikes(node.path("likeCount").asLong(), node.path("likedByViewer").asBoolean())));
            return byId;
        }

        @Override
        public Comment comment(String postId, String body) {
            HttpResponse<String> response = call("POST", "/api/posts/" + postId + "/comments",
                json.writeValueAsString(Map.of("body", body)));
            require(response, 201, "add a comment");
            return toComment(json.readTree(response.body()));
        }

        @Override
        public CommentPage commentsOn(String postId, String cursor, Integer limit) {
            var query = new StringBuilder();
            if (cursor != null) {
                query.append(query.isEmpty() ? '?' : '&').append("cursor=").append(cursor);
            }
            if (limit != null) {
                query.append(query.isEmpty() ? '?' : '&').append("limit=").append(limit);
            }
            HttpResponse<String> response = call("GET", "/api/posts/" + postId + "/comments" + query, null);
            require(response, 200, "read a comment thread");
            JsonNode page = json.readTree(response.body());
            List<Comment> comments = new ArrayList<>();
            page.path("items").forEach(node -> comments.add(toComment(node)));
            JsonNode next = page.path("nextCursor");
            return new CommentPage(comments, next.isNull() || next.isMissingNode() ? null : next.asString());
        }

        @Override
        public DeleteOutcome deleteComment(String commentId) {
            HttpResponse<String> response = call("DELETE", "/api/comments/" + commentId, null);
            return switch (response.statusCode()) {
                case 204 -> DeleteOutcome.DELETED;
                case 403 -> DeleteOutcome.FORBIDDEN;
                default -> throw new AssertionError(
                    "Unexpected status deleting a comment: " + response.statusCode() + ": " + response.body());
            };
        }

        @Override
        public Map<String, Long> commentCountsOf(String... postIds) {
            String query = Arrays.stream(postIds).map(id -> "postIds=" + id).collect(Collectors.joining("&"));
            HttpResponse<String> response = call("GET", "/api/comments?" + query, null);
            require(response, 200, "read a batch of post comment counts");
            Map<String, Long> byId = new LinkedHashMap<>();
            json.readTree(response.body())
                .forEach(node -> byId.put(node.path("postId").asString(), node.path("commentCount").asLong()));
            return byId;
        }

        @Override
        public FeedPage openFeed() {
            return openFeed(null, null);
        }

        @Override
        public FeedPage openFeed(String cursor, Integer limit) {
            var query = new StringBuilder();
            if (cursor != null) {
                query.append(query.isEmpty() ? '?' : '&').append("cursor=").append(cursor);
            }
            if (limit != null) {
                query.append(query.isEmpty() ? '?' : '&').append("limit=").append(limit);
            }
            HttpResponse<String> response = call("GET", "/api/feed" + query, null);
            require(response, 200, "open feed");
            JsonNode page = json.readTree(response.body());
            List<Post> posts = new ArrayList<>();
            page.path("items").forEach(node -> posts.add(post(node)));
            JsonNode next = page.path("nextCursor");
            return new FeedPage(posts, next.isNull() || next.isMissingNode() ? null : next.asString());
        }

        private HttpResponse<String> call(String method, String path, String body) {
            var request = HttpRequest.newBuilder(uri(path)).header("Authorization", "Bearer " + accessToken)
                .method(method, body == null ? BodyPublishers.noBody() : BodyPublishers.ofString(body));
            if (body != null) {
                request.header("Content-Type", "application/json");
            }
            return send(http, request.build());
        }
    }

    private Profile profile(String body) {
        JsonNode node = json.readTree(body);
        return new Profile(node.path("userId").asString(), node.path("username").asString(),
            textOrNull(node, "displayName"), textOrNull(node, "bio"));
    }

    private Post post(String body) {
        return post(json.readTree(body));
    }

    private static Comment toComment(JsonNode node) {
        return new Comment(node.path("commentId").asString(), node.path("postId").asString(),
            node.path("authorId").asString(), node.path("body").asString(), node.path("createdAt").asString());
    }

    private static Post post(JsonNode node) {
        return new Post(node.path("postId").asString(), node.path("authorId").asString(),
            node.path("mediaId").asString(), textOrNull(node, "caption"), node.path("publishedAt").asString());
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
}
