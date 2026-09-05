package me.imshy.pictogram;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class ProfileReadApiTest {

    @Autowired
    MockMvc mvc;

    private String onboard(String username, String displayName) throws Exception {
        var user = UUID.randomUUID().toString();
        mvc.perform(
            post("/api/profiles").with(jwt().jwt(jwt -> jwt.subject(user))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"displayName\":\"%s\"}".formatted(username, displayName)))
            .andExpect(status().isCreated());
        return user;
    }

    @Test
    void anyoneCanReadAProfileByUsernameWithoutAToken() throws Exception {
        var ada = onboard("ada_lovelace", "Ada Lovelace");

        mvc.perform(get("/api/profiles/ada_lovelace")).andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(ada)).andExpect(jsonPath("$.username").value("ada_lovelace"))
            .andExpect(jsonPath("$.displayName").value("Ada Lovelace"));
    }

    @Test
    void anUnknownUsernameIsAProfileNotFoundProblemDetail() throws Exception {
        mvc.perform(get("/api/profiles/nobody_here")).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type").value(ProblemType.BASE + "profile-not-found"));
    }

    @Test
    void aProfileLookupByUsernameIgnoresCasing() throws Exception {
        onboard("ada_lovelace", "Ada Lovelace");

        mvc.perform(get("/api/profiles/Ada_Lovelace")).andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("ada_lovelace"));
    }

    @Test
    void theBatchLookupResolvesKnownIdsInOneCallAndOmitsTheRest() throws Exception {
        var ada = onboard("ada", "Ada");
        var grace = onboard("grace", "Grace");
        var missing = UUID.randomUUID().toString();

        mvc.perform(get("/api/profiles").param("ids", ada, missing, grace).with(jwt().jwt(jwt -> jwt.subject(ada))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[?(@.username == 'ada')]").exists())
            .andExpect(jsonPath("$[?(@.username == 'grace')]").exists());
    }

    @Test
    void theBatchLookupRejectsMoreIdsThanTheBatchLimit() throws Exception {
        String[] tooMany = IntStream.rangeClosed(0, 100).mapToObj(i -> UUID.randomUUID().toString())
            .toArray(String[]::new);

        mvc.perform(get("/api/profiles").param("ids", tooMany)
            .with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString())))).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value(ProblemType.OVERSIZED_BATCH.uri().toString()));
    }

    @Test
    void theCarveOutIsOneSegmentWideSoBatchAndMeStillNeedAToken() throws Exception {
        mvc.perform(get("/api/profiles/me")).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.type").value(ProblemType.UNAUTHORIZED.uri().toString()));

        mvc.perform(get("/api/profiles").param("ids", UUID.randomUUID().toString()))
            .andExpect(status().isUnauthorized());
    }
}
