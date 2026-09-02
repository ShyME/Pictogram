package me.imshy.pictogram;

import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.stream.IntStream;
import me.imshy.pictogram.shared.http.ProblemType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@AppIntegrationTest
class LikeApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void likingRequiresAToken() throws Exception {
        mvc.perform(put("/api/engagement/likes/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.type").value(ProblemType.UNAUTHORIZED.uri().toString()));
    }

    @Test
    void aSignedInViewerLikesAPostAndTheBatchReadReflectsIt() throws Exception {
        var ada = UUID.randomUUID().toString();
        var post = UUID.randomUUID().toString();

        mvc.perform(put("/api/engagement/likes/" + post).with(jwt().jwt(jwt -> jwt.subject(ada))))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/engagement/likes").param("postIds", post).with(jwt().jwt(jwt -> jwt.subject(ada))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].postId").value(post))
                .andExpect(jsonPath("$[0].likeCount").value(1))
                .andExpect(jsonPath("$[0].likedByViewer").value(true));
    }

    @Test
    void likingIsIdempotentAndUnlikingRemovesTheLike() throws Exception {
        var ada = UUID.randomUUID().toString();
        var post = UUID.randomUUID().toString();

        mvc.perform(put("/api/engagement/likes/" + post).with(jwt().jwt(jwt -> jwt.subject(ada))))
                .andExpect(status().isNoContent());
        mvc.perform(put("/api/engagement/likes/" + post).with(jwt().jwt(jwt -> jwt.subject(ada))))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/engagement/likes/" + post).with(jwt().jwt(jwt -> jwt.subject(ada))))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/engagement/likes/" + post).with(jwt().jwt(jwt -> jwt.subject(ada))))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/engagement/likes").param("postIds", post).with(jwt().jwt(jwt -> jwt.subject(ada))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].likeCount").value(0))
                .andExpect(jsonPath("$[0].likedByViewer").value(false));
    }

    @Test
    void theBatchReadRequiresAToken() throws Exception {
        mvc.perform(get("/api/engagement/likes")
                        .param("postIds", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void theBatchReadReturnsARecordPerPostWithTheCountAndTheViewerFlag() throws Exception {
        var ada = UUID.randomUUID().toString();
        var bob = UUID.randomUUID().toString();
        var liked = UUID.randomUUID().toString();
        var likedByBobOnly = UUID.randomUUID().toString();

        mvc.perform(put("/api/engagement/likes/" + liked).with(jwt().jwt(jwt -> jwt.subject(ada))))
                .andExpect(status().isNoContent());
        mvc.perform(put("/api/engagement/likes/" + liked).with(jwt().jwt(jwt -> jwt.subject(bob))))
                .andExpect(status().isNoContent());
        mvc.perform(put("/api/engagement/likes/" + likedByBobOnly).with(jwt().jwt(jwt -> jwt.subject(bob))))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/engagement/likes")
                        .param("postIds", liked, likedByBobOnly)
                        .with(jwt().jwt(jwt -> jwt.subject(ada))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(
                        jsonPath("$[?(@.postId == '" + liked + "')].likeCount").value(contains(2)))
                .andExpect(jsonPath("$[?(@.postId == '" + liked + "')].likedByViewer")
                        .value(contains(true)))
                .andExpect(jsonPath("$[?(@.postId == '" + likedByBobOnly + "')].likeCount")
                        .value(contains(1)))
                .andExpect(jsonPath("$[?(@.postId == '" + likedByBobOnly + "')].likedByViewer")
                        .value(contains(false)));
    }

    @Test
    void theBatchReadRejectsMoreIdsThanTheBatchLimit() throws Exception {
        String[] tooMany = IntStream.rangeClosed(0, 100)
                .mapToObj(i -> UUID.randomUUID().toString())
                .toArray(String[]::new);

        mvc.perform(get("/api/engagement/likes")
                        .param("postIds", tooMany)
                        .with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type")
                        .value(ProblemType.OVERSIZED_BATCH.uri().toString()));
    }
}
