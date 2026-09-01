package me.imshy.pictogram;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The committed {@code openapi.json} must describe the SPA-facing auth and profile endpoints
 * as they actually behave — the real status codes, the {@code Location} header, and the
 * Problem Detail error bodies — so the generated TypeScript client carries them instead of
 * the frontend hand-declaring response shapes and branching on raw status numbers (#35).
 *
 * <p>Reads the file directly rather than booting the app: {@link OpenApiContractTest}
 * already guarantees the file equals what the running app publishes.
 */
class OpenApiDocumentationTest {

    private static JsonNode spec;

    @BeforeAll
    static void readSpec() throws IOException {
        Path file = Path.of(System.getProperty("pictogram.openapi.file", "../openapi.json"));
        spec = JsonMapper.builder().build().readTree(Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void onboardingIsDocumentedAs201WithLocationAndProblemDetailErrors() {
        JsonNode onboard = spec.at("/paths/~1api~1profiles/post/responses");

        assertThat(onboard.has("200")).as("no phantom 200").isFalse();
        assertThat(onboard.at("/201/headers/Location")).isNotEmpty();
        assertThat(onboard.at("/201/content/application~1json/schema/$ref").asString())
                .endsWith("/ProfileView");
        assertThat(onboard.at("/400/content/application~1problem+json/schema/$ref").asString())
                .endsWith("/ProblemDetail");
        assertThat(onboard.at("/409/content/application~1problem+json/schema/$ref").asString())
                .endsWith("/ProblemDetail");
    }

    @Test
    void ownProfileIsDocumentedWith200And404() {
        JsonNode me = spec.at("/paths/~1api~1profiles~1me/get/responses");

        assertThat(me.at("/200/content/application~1json/schema/$ref").asString()).endsWith("/ProfileView");
        assertThat(me.at("/404/content/application~1problem+json/schema/$ref").asString())
                .endsWith("/ProblemDetail");
    }

    @Test
    void editingOwnProfileIsDocumentedWith200AndProblemDetailErrors() {
        JsonNode edit = spec.at("/paths/~1api~1profiles~1me/put/responses");

        assertThat(edit.at("/200/content/application~1json/schema/$ref").asString()).endsWith("/ProfileView");
        assertThat(edit.at("/400/content/application~1problem+json/schema/$ref").asString())
                .endsWith("/ProblemDetail");
        assertThat(edit.at("/404/content/application~1problem+json/schema/$ref").asString())
                .endsWith("/ProblemDetail");
        assertThat(edit.at("/409/content/application~1problem+json/schema/$ref").asString())
                .endsWith("/ProblemDetail");
    }

    @Test
    void profileByUsernameIsDocumentedWith200And404() {
        JsonNode byUsername = spec.at("/paths/~1api~1profiles~1{username}/get/responses");

        assertThat(byUsername.at("/200/content/application~1json/schema/$ref").asString()).endsWith("/ProfileView");
        assertThat(byUsername.at("/404/content/application~1problem+json/schema/$ref").asString())
                .endsWith("/ProblemDetail");
    }

    @Test
    void theBatchProfileLookupIsDocumentedAsAnArrayOfProfileView() {
        JsonNode byIds = spec.at("/paths/~1api~1profiles/get/responses");

        assertThat(byIds.at("/200/content/application~1json/schema/type").asString()).isEqualTo("array");
        assertThat(byIds.at("/200/content/application~1json/schema/items/$ref").asString()).endsWith("/ProfileView");
    }

    @Test
    void mediaUploadIsDocumentedAs201WithAMediaIdAndA400ProblemDetail() {
        JsonNode upload = spec.at("/paths/~1api~1media/post/responses");

        assertThat(upload.at("/201/content/application~1json/schema/$ref").asString())
                .endsWith("/MediaUploadResponse");
        assertThat(upload.at("/400/content/application~1problem+json/schema/$ref").asString())
                .endsWith("/ProblemDetail");
    }

    @Test
    void publishingAPostIsDocumentedAs201WithLocationAnd400And422ProblemDetails() {
        JsonNode publish = spec.at("/paths/~1api~1posts/post/responses");

        assertThat(publish.has("200")).as("no phantom 200").isFalse();
        assertThat(publish.at("/201/headers/Location")).isNotEmpty();
        assertThat(publish.at("/201/content/application~1json/schema/$ref").asString()).endsWith("/PostView");
        assertThat(publish.at("/400/content/application~1problem+json/schema/$ref").asString())
                .endsWith("/ProblemDetail");
        assertThat(publish.at("/422/content/application~1problem+json/schema/$ref").asString())
                .endsWith("/ProblemDetail");
    }

    @Test
    void deletingAPostIsDocumentedAs204With403And404ProblemDetails() {
        JsonNode delete = spec.at("/paths/~1api~1posts~1{postId}/delete/responses");

        assertThat(delete.has("204")).isTrue();
        assertThat(delete.has("200")).as("no phantom 200").isFalse();
        assertThat(delete.at("/403/content/application~1problem+json/schema/$ref").asString())
                .endsWith("/ProblemDetail");
        assertThat(delete.at("/404/content/application~1problem+json/schema/$ref").asString())
                .endsWith("/ProblemDetail");
    }

    @Test
    void thePostGridIsDocumentedAsAPageOfPostView() {
        JsonNode grid = spec.at("/paths/~1api~1posts/get/responses/200/content/application~1json/schema/$ref");

        JsonNode page = spec.at("/components/schemas/" + grid.asString().substring("#/components/schemas/".length()));
        assertThat(page.at("/properties/items/items/$ref").asString()).endsWith("/PostView");
        assertThat(page.at("/properties/nextCursor")).isNotEmpty();
    }

    @Test
    void servingAMediaRenditionIsDocumentedAsBinaryJpegWith404() {
        JsonNode original = spec.at("/paths/~1api~1media~1{mediaId}~1original/get/responses");

        assertThat(original.at("/200/content/image~1jpeg/schema/format").asString()).isEqualTo("binary");
        assertThat(original.at("/404/content/application~1problem+json/schema/$ref").asString())
                .endsWith("/ProblemDetail");
    }

    @Test
    void followingAUserIsDocumentedAs204WithA422SelfFollowProblemDetail() {
        JsonNode follow = spec.at("/paths/~1api~1follows~1{userId}/put/responses");

        assertThat(follow.has("204")).isTrue();
        assertThat(follow.has("200")).as("no phantom 200").isFalse();
        assertThat(follow.at("/422/content/application~1problem+json/schema/$ref").asString())
                .endsWith("/ProblemDetail");
    }

    @Test
    void unfollowingAUserIsDocumentedAs204() {
        JsonNode unfollow = spec.at("/paths/~1api~1follows~1{userId}/delete/responses");

        assertThat(unfollow.has("204")).isTrue();
        assertThat(unfollow.has("200")).as("no phantom 200").isFalse();
    }

    @Test
    void theFollowRelationshipIsDocumentedWithCountsAndTheViewerFlag() {
        JsonNode relationship = spec.at(
                "/paths/~1api~1follows~1{userId}/get/responses/200/content/application~1json/schema/$ref");

        JsonNode body = spec.at("/components/schemas/"
                + relationship.asString().substring("#/components/schemas/".length()));
        assertThat(body.at("/properties/followerCount")).isNotEmpty();
        assertThat(body.at("/properties/followingCount")).isNotEmpty();
        assertThat(body.at("/properties/followedByViewer")).isNotEmpty();
    }

    @Test
    void theBatchRelationshipReadIsDocumentedAsAnArrayOfRecordsNeedingAToken() {
        JsonNode byIds = spec.at("/paths/~1api~1follows/get/responses");

        assertThat(byIds.at("/200/content/application~1json/schema/type").asString()).isEqualTo("array");
        String itemRef = byIds.at("/200/content/application~1json/schema/items/$ref").asString();
        JsonNode item = spec.at("/components/schemas/" + itemRef.substring("#/components/schemas/".length()));
        assertThat(item.at("/properties/userId")).isNotEmpty();
        assertThat(item.at("/properties/followerCount")).isNotEmpty();
        assertThat(item.at("/properties/followingCount")).isNotEmpty();
        assertThat(item.at("/properties/followedByViewer")).isNotEmpty();
        assertThat(byIds.at("/400/content/application~1problem+json/schema/$ref").asString()).endsWith("/ProblemDetail");
        assertThat(byIds.at("/401/content/application~1problem+json/schema/$ref").asString()).endsWith("/ProblemDetail");
    }

    @Test
    void theFollowerListIsDocumentedAsAPageOfUserIdsNeedingAToken() {
        JsonNode followers = spec.at("/paths/~1api~1follows~1{userId}~1followers/get/responses");

        String pageRef = followers.at("/200/content/application~1json/schema/$ref").asString();
        JsonNode page = spec.at("/components/schemas/" + pageRef.substring("#/components/schemas/".length()));
        assertThat(page.at("/properties/items/items/type").asString()).isEqualTo("string");
        assertThat(page.at("/properties/nextCursor")).isNotEmpty();
        assertThat(followers.at("/401/content/application~1problem+json/schema/$ref").asString())
                .endsWith("/ProblemDetail");
    }

    @Test
    void theFollowingListIsDocumentedAsAPageOfUserIdsNeedingAToken() {
        JsonNode following = spec.at("/paths/~1api~1follows~1{userId}~1following/get/responses");

        String pageRef = following.at("/200/content/application~1json/schema/$ref").asString();
        JsonNode page = spec.at("/components/schemas/" + pageRef.substring("#/components/schemas/".length()));
        assertThat(page.at("/properties/items/items/type").asString()).isEqualTo("string");
        assertThat(following.at("/401/content/application~1problem+json/schema/$ref").asString())
                .endsWith("/ProblemDetail");
    }

    @Test
    void refreshIsDocumentedWithATypedBodyAndA401() {
        JsonNode refresh = spec.at("/paths/~1api~1auth~1refresh/post/responses");

        String okRef = refresh.at("/200/content/application~1json/schema/$ref").asString();
        assertThat(okRef).describedAs("200 body is a named schema, not type:object").endsWith("/AccessTokenResponse");
        JsonNode body = spec.at("/components/schemas/AccessTokenResponse");
        assertThat(body.at("/properties/accessToken")).isNotEmpty();
        assertThat(body.at("/properties/expiresInSeconds")).isNotEmpty();

        assertThat(refresh.at("/401/content/application~1problem+json/schema/$ref").asString())
                .endsWith("/ProblemDetail");
    }

    @Test
    void logoutIsDocumentedAs204() {
        JsonNode logout = spec.at("/paths/~1api~1auth~1logout/post/responses");

        assertThat(logout.has("204")).isTrue();
        assertThat(logout.has("200")).as("no phantom 200").isFalse();
    }
}
