package me.imshy.pictogram;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import me.imshy.pictogram.shared.http.ProblemType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@AppIntegrationTest
class FeedApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void theFeedNeedsAToken() throws Exception {
        mvc.perform(get("/api/feed")).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.type").value(ProblemType.UNAUTHORIZED.uri().toString()));
    }

    @Test
    void aSignedInViewerWhoFollowsNobodyGetsAnEmptyLastPage() throws Exception {
        mvc.perform(get("/api/feed").with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty())
            .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void aMalformedCursorIsRejected() throws Exception {
        mvc.perform(
            get("/api/feed?cursor=not-a-cursor").with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value(ProblemType.INVALID_CURSOR.uri().toString()));
    }
}
