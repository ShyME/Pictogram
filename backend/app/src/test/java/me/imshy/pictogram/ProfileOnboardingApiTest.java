package me.imshy.pictogram;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;
import me.imshy.pictogram.shared.http.ProblemType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AppIntegrationTest
class ProfileOnboardingApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void ownProfileIs404BeforeOnboardingAndReturnsTheProfileAfter() throws Exception {
        var user = jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()));

        mvc.perform(get("/api/profiles/me").with(user)).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type").value(ProblemType.BASE + "profile-not-found"));

        mvc.perform(post("/api/profiles").with(user).contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"ada_lovelace\"}")).andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/profiles/ada_lovelace"))
            .andExpect(jsonPath("$.username").value("ada_lovelace"));

        mvc.perform(get("/api/profiles/me").with(user)).andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("ada_lovelace"));
    }

    @Test
    void aMalformedUsernameAndaTakenUsernameAreDistinctProblemDetails() throws Exception {
        mvc.perform(post("/api/profiles").with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString())))
            .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"No Good\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value(ProblemType.BASE + "username-invalid"));

        mvc.perform(post("/api/profiles").with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString())))
            .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"grace\"}"))
            .andExpect(status().isCreated());

        mvc.perform(post("/api/profiles").with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString())))
            .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"grace\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.type").value(ProblemType.BASE + "username-taken"));
    }

    @Test
    void onboardingTwiceForTheSameUserIsRejected() throws Exception {
        var user = jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()));
        mvc.perform(
            post("/api/profiles").with(user).contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"ada\"}"))
            .andExpect(status().isCreated());

        mvc.perform(post("/api/profiles").with(user).contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"ada_again\"}")).andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value(ProblemType.BASE + "already-onboarded"));
    }
}
