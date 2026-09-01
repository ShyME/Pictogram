package me.imshy.pictogram;

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
}
