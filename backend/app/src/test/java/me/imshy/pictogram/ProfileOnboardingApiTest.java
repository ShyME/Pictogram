package me.imshy.pictogram;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = PictogramApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileOnboardingApiTest {

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
    void ownProfileIs404BeforeOnboardingAndReturnsTheProfileAfter() throws Exception {
        var user = jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()));

        mvc.perform(get("/api/profiles/me").with(user))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(ProblemType.BASE + "profile-not-found"));

        mvc.perform(post("/api/profiles")
                        .with(user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ada_lovelace\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/profiles/ada_lovelace"))
                .andExpect(jsonPath("$.username").value("ada_lovelace"));

        mvc.perform(get("/api/profiles/me").with(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ada_lovelace"));
    }

    @Test
    void aMalformedUsernameAndaTakenUsernameAreDistinctProblemDetails() throws Exception {
        mvc.perform(post("/api/profiles")
                        .with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"No Good\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ProblemType.BASE + "username-invalid"));

        mvc.perform(post("/api/profiles")
                        .with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"grace\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/profiles")
                        .with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"grace\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ProblemType.BASE + "username-taken"));
    }

    @Test
    void onboardingTwiceForTheSameUserIsRejected() throws Exception {
        var user = jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()));
        mvc.perform(post("/api/profiles")
                        .with(user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ada\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/profiles")
                        .with(user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ada_again\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ProblemType.BASE + "already-onboarded"));
    }
}
