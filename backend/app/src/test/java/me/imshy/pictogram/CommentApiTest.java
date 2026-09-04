package me.imshy.pictogram;

import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.stream.IntStream;
import me.imshy.pictogram.shared.http.ProblemType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AppIntegrationTest
class CommentApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void commentingRequiresAToken() throws Exception {
        mvc.perform(post("/api/posts/" + UUID.randomUUID() + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"hi\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.type").value(ProblemType.UNAUTHORIZED.uri().toString()));
    }

    @Test
    void aSignedInViewerAddsACommentAndSeesItInTheThread() throws Exception {
        var ada = UUID.randomUUID().toString();
        var post = UUID.randomUUID().toString();

        mvc.perform(post("/api/posts/" + post + "/comments")
                        .with(jwt().jwt(jwt -> jwt.subject(ada)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"nice shot\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/comments/")))
                .andExpect(jsonPath("$.postId").value(post))
                .andExpect(jsonPath("$.authorId").value(ada))
                .andExpect(jsonPath("$.body").value("nice shot"))
                .andExpect(jsonPath("$.commentId").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        mvc.perform(get("/api/posts/" + post + "/comments").with(jwt().jwt(jwt -> jwt.subject(ada))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].body").value("nice shot"));
    }

    @Test
    void theThreadReadServesAnAnonymousCaller() throws Exception {
        var ada = UUID.randomUUID().toString();
        var post = UUID.randomUUID().toString();

        mvc.perform(post("/api/posts/" + post + "/comments")
                        .with(jwt().jwt(jwt -> jwt.subject(ada)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"visible to all\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/posts/" + post + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].body").value("visible to all"));
    }

    @Test
    void theThreadReadPagesOldestFirst() throws Exception {
        var ada = UUID.randomUUID().toString();
        var post = UUID.randomUUID().toString();

        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/posts/" + post + "/comments")
                            .with(jwt().jwt(jwt -> jwt.subject(ada)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"body\":\"comment " + i + "\"}"))
                    .andExpect(status().isCreated());
        }

        var firstPage = mvc.perform(get("/api/posts/" + post + "/comments").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].body").value("comment 0"))
                .andExpect(jsonPath("$.items[1].body").value("comment 1"))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty())
                .andReturn();

        String cursor =
                com.jayway.jsonpath.JsonPath.read(firstPage.getResponse().getContentAsString(), "$.nextCursor");

        mvc.perform(get("/api/posts/" + post + "/comments")
                        .param("cursor", cursor)
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].body").value("comment 2"))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void anEmptyCommentIsRejected() throws Exception {
        mvc.perform(post("/api/posts/" + UUID.randomUUID() + "/comments")
                        .with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://pictogram.dev/problems/comment-empty"));
    }

    @Test
    void aCommentOverTheLengthLimitIsRejected() throws Exception {
        String tooLong = "x".repeat(1001);

        mvc.perform(post("/api/posts/" + UUID.randomUUID() + "/comments")
                        .with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://pictogram.dev/problems/comment-too-long"));
    }

    @Test
    void aMalformedCursorIsRejected() throws Exception {
        mvc.perform(get("/api/posts/" + UUID.randomUUID() + "/comments").param("cursor", "not-a-cursor"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type")
                        .value(ProblemType.INVALID_CURSOR.uri().toString()));
    }

    @Test
    void deletingACommentRequiresAToken() throws Exception {
        mvc.perform(delete("/api/comments/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.type").value(ProblemType.UNAUTHORIZED.uri().toString()));
    }

    @Test
    void aCommentsAuthorDeletesItAndItLeavesTheThread() throws Exception {
        var ada = UUID.randomUUID().toString();
        var post = UUID.randomUUID().toString();

        var created = mvc.perform(post("/api/posts/" + post + "/comments")
                        .with(jwt().jwt(jwt -> jwt.subject(ada)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"my bad\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String commentId =
                com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.commentId");

        mvc.perform(delete("/api/comments/" + commentId).with(jwt().jwt(jwt -> jwt.subject(ada))))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/posts/" + post + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void aStrangerCannotDeleteSomeoneElsesCommentOnAPostTheyDoNotOwn() throws Exception {
        var ada = UUID.randomUUID().toString();
        var stranger = UUID.randomUUID().toString();
        var post = UUID.randomUUID().toString();

        var created = mvc.perform(post("/api/posts/" + post + "/comments")
                        .with(jwt().jwt(jwt -> jwt.subject(ada)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"hands off\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String commentId =
                com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.commentId");

        mvc.perform(delete("/api/comments/" + commentId).with(jwt().jwt(jwt -> jwt.subject(stranger))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value(ProblemType.FORBIDDEN.uri().toString()));
    }

    @Test
    void deletingACommentThatIsNotThereIsAnIdempotent204() throws Exception {
        mvc.perform(delete("/api/comments/" + UUID.randomUUID())
                        .with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isNoContent());
    }

    @Test
    void theBatchCountReadReturnsARecordPerPostAndServesAnAnonymousCaller() throws Exception {
        var ada = UUID.randomUUID().toString();
        var chatty = UUID.randomUUID().toString();
        var quiet = UUID.randomUUID().toString();

        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/posts/" + chatty + "/comments")
                            .with(jwt().jwt(jwt -> jwt.subject(ada)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"body\":\"c" + i + "\"}"))
                    .andExpect(status().isCreated());
        }

        mvc.perform(get("/api/comments").param("postIds", chatty, quiet))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.postId == '" + chatty + "')].commentCount")
                        .value(contains(2)))
                .andExpect(jsonPath("$[?(@.postId == '" + quiet + "')].commentCount")
                        .value(contains(0)));
    }

    @Test
    void theBatchCountReadRejectsMoreIdsThanTheBatchLimit() throws Exception {
        String[] tooMany = IntStream.rangeClosed(0, 100)
                .mapToObj(i -> UUID.randomUUID().toString())
                .toArray(String[]::new);

        mvc.perform(get("/api/comments")
                        .param("postIds", tooMany)
                        .with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type")
                        .value(ProblemType.OVERSIZED_BATCH.uri().toString()));
    }
}
