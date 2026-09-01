package me.imshy.pictogram;

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
import javax.sql.DataSource;
import me.imshy.pictogram.shared.http.ProblemType;
import me.imshy.pictogram.testsupport.DatabaseCleaner;
import me.imshy.pictogram.testsupport.SharedPostgres;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The follow endpoints through the real security chain: {@code PUT} / {@code DELETE} need a
 * token and always act as the caller; {@code GET /api/follows/{userId}} is public and
 * reports counts to everyone but the {@code followedByViewer} flag only to a signed-in
 * viewer; a self-follow is a {@code 422} Problem Detail.
 */
@SpringBootTest(classes = PictogramApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FollowApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    DataSource dataSource;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        SharedPostgres.registerTo(registry);
    }

    @AfterEach
    void truncateAllTables() {
        new DatabaseCleaner(dataSource).truncateAll();
    }

    @Test
    void followingRequiresAToken() throws Exception {
        mvc.perform(put("/api/follows/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value(ProblemType.UNAUTHORIZED.uri().toString()));
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

        mvc.perform(get("/api/follows/" + user + "/followers"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/follows/" + user + "/following"))
                .andExpect(status().isUnauthorized());
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

        // Order (newest follow first) is asserted deterministically in follow's FollowListTest,
        // which controls the clock; here the two follows race the real clock, so assert membership.
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
    void theFollowerListPagesOnAnOpaqueCursor() throws Exception {
        var target = UUID.randomUUID();
        for (int i = 0; i < 3; i++) {
            mvc.perform(put("/api/follows/" + target)
                            .with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()))))
                    .andExpect(status().isNoContent());
        }
        var viewer = jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()));

        String firstPage = mvc.perform(get("/api/follows/" + target + "/followers?limit=2").with(viewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String cursor = com.jayway.jsonpath.JsonPath.read(firstPage, "$.nextCursor");

        mvc.perform(get("/api/follows/" + target + "/followers?limit=2&cursor=" + cursor).with(viewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.nextCursor").value(nullValue()));
    }
}
