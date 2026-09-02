package me.imshy.pictogram;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
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
class FollowApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void followingRequiresAToken() throws Exception {
        mvc.perform(put("/api/follows/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.type").value(ProblemType.UNAUTHORIZED.uri().toString()));
    }

    @Test
    void aSignedInViewerFollowsAUserAndTheCountsAndRelationshipReflectIt() throws Exception {
        var ada = UUID.randomUUID().toString();
        var bob = UUID.randomUUID();

        mvc.perform(put("/api/follows/" + bob).with(jwt().jwt(jwt -> jwt.subject(ada))))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/follows/" + bob).with(jwt().jwt(jwt -> jwt.subject(ada))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followerCount").value(1))
                .andExpect(jsonPath("$.followingCount").value(0))
                .andExpect(jsonPath("$.followedByViewer").value(true));
    }

    @Test
    void followingIsIdempotentAndUnfollowingRemovesTheEdge() throws Exception {
        var ada = UUID.randomUUID().toString();
        var bob = UUID.randomUUID();

        mvc.perform(put("/api/follows/" + bob).with(jwt().jwt(jwt -> jwt.subject(ada))))
                .andExpect(status().isNoContent());
        mvc.perform(put("/api/follows/" + bob).with(jwt().jwt(jwt -> jwt.subject(ada))))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/follows/" + bob).with(jwt().jwt(jwt -> jwt.subject(ada))))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/follows/" + bob).with(jwt().jwt(jwt -> jwt.subject(ada))))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/follows/" + bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followerCount").value(0));
    }

    @Test
    void aSelfFollowIsA422ProblemDetail() throws Exception {
        var ada = UUID.randomUUID();

        mvc.perform(put("/api/follows/" + ada).with(jwt().jwt(jwt -> jwt.subject(ada.toString()))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value(ProblemType.BASE + "self-follow"));
    }

    @Test
    void theCountsAreReadableWithoutATokenButTheRelationshipIsNot() throws Exception {
        var ada = UUID.randomUUID().toString();
        var bob = UUID.randomUUID();

        mvc.perform(put("/api/follows/" + bob).with(jwt().jwt(jwt -> jwt.subject(ada))))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/follows/" + bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followerCount").value(1))
                .andExpect(jsonPath("$.followedByViewer").value(false));
    }

    @Test
    void theFollowerAndFollowingListsNeedAToken() throws Exception {
        var user = UUID.randomUUID();

        mvc.perform(get("/api/follows/" + user + "/followers")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/follows/" + user + "/following")).andExpect(status().isUnauthorized());
    }

    @Test
    void aSignedInViewerReadsWhoFollowsAUserAndWhoTheyFollow() throws Exception {
        var ada = UUID.randomUUID().toString();
        var bob = UUID.randomUUID().toString();
        var carol = UUID.randomUUID();

        mvc.perform(put("/api/follows/" + carol).with(jwt().jwt(jwt -> jwt.subject(ada))))
                .andExpect(status().isNoContent());
        mvc.perform(put("/api/follows/" + carol).with(jwt().jwt(jwt -> jwt.subject(bob))))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/follows/" + carol + "/followers").with(jwt().jwt(jwt -> jwt.subject(ada))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items", containsInAnyOrder(ada, bob)))
                .andExpect(jsonPath("$.nextCursor").value(nullValue()));

        mvc.perform(get("/api/follows/" + ada + "/following").with(jwt().jwt(jwt -> jwt.subject(ada))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0]").value(carol.toString()));
    }

    @Test
    void theBatchRelationshipReadNeedsAToken() throws Exception {
        mvc.perform(get("/api/follows").param("ids", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void theBatchRelationshipReadReturnsARecordPerIdWithCountsAndTheViewerFlag() throws Exception {
        var ada = UUID.randomUUID().toString();
        var bob = UUID.randomUUID().toString();
        var carol = UUID.randomUUID().toString();

        mvc.perform(put("/api/follows/" + bob).with(jwt().jwt(jwt -> jwt.subject(ada))))
                .andExpect(status().isNoContent());
        mvc.perform(put("/api/follows/" + carol).with(jwt().jwt(jwt -> jwt.subject(bob))))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/follows").param("ids", bob, carol).with(jwt().jwt(jwt -> jwt.subject(ada))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.userId == '" + bob + "')].followerCount")
                        .value(contains(1)))
                .andExpect(jsonPath("$[?(@.userId == '" + bob + "')].followingCount")
                        .value(contains(1)))
                .andExpect(jsonPath("$[?(@.userId == '" + bob + "')].followedByViewer")
                        .value(contains(true)))
                .andExpect(jsonPath("$[?(@.userId == '" + carol + "')].followerCount")
                        .value(contains(1)))
                .andExpect(jsonPath("$[?(@.userId == '" + carol + "')].followedByViewer")
                        .value(contains(false)));
    }

    @Test
    void theBatchRelationshipReadRejectsMoreIdsThanTheBatchLimit() throws Exception {
        String[] tooMany = IntStream.rangeClosed(0, 100)
                .mapToObj(i -> UUID.randomUUID().toString())
                .toArray(String[]::new);

        mvc.perform(get("/api/follows")
                        .param("ids", tooMany)
                        .with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type")
                        .value(ProblemType.OVERSIZED_BATCH.uri().toString()));
    }

    @Test
    void theFollowerListPagesOnAnOpaqueCursor() throws Exception {
        var target = UUID.randomUUID();
        for (int i = 0; i < 3; i++) {
            mvc.perform(put("/api/follows/" + target)
                            .with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()))))
                    .andExpect(status().isNoContent());
        }
        var viewer = jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()));

        String firstPage = mvc.perform(
                        get("/api/follows/" + target + "/followers?limit=2").with(viewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String cursor = com.jayway.jsonpath.JsonPath.read(firstPage, "$.nextCursor");

        mvc.perform(get("/api/follows/" + target + "/followers?limit=2&cursor=" + cursor)
                        .with(viewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.nextCursor").value(nullValue()));
    }
}
