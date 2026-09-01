package me.imshy.pictogram;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = PictogramApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileEditApiTest {

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

    private static org.springframework.test.web.servlet.request.RequestPostProcessor asUser(String subject) {
        return jwt().jwt(jwt -> jwt.subject(subject));
    }

    private void onboard(String subject, String username) throws Exception {
        mvc.perform(post("/api/profiles").with(asUser(subject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\"}".formatted(username)))
                .andExpect(status().isCreated());
    }

    @Test
    void editingUpdatesTheDisplayNameBioAndUsername() throws Exception {
        var user = UUID.randomUUID().toString();
        onboard(user, "ada");

        mvc.perform(put("/api/profiles/me").with(asUser(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ada_lovelace\",\"displayName\":\"Ada Lovelace\",\"bio\":\"Countess\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ada_lovelace"))
                .andExpect(jsonPath("$.displayName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.bio").value("Countess"));

        mvc.perform(get("/api/profiles/me").with(asUser(user)))
                .andExpect(jsonPath("$.username").value("ada_lovelace"));
    }

    @Test
    void editingBeforeOnboardingIsAProfileNotFoundProblemDetail() throws Exception {
        mvc.perform(put("/api/profiles/me").with(asUser(UUID.randomUUID().toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ada\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(ProblemType.BASE + "profile-not-found"));
    }

    @Test
    void aMalformedNewUsernameAndaTakenOneAreDistinctProblemDetails() throws Exception {
        var ada = UUID.randomUUID().toString();
        onboard(ada, "ada");
        onboard(UUID.randomUUID().toString(), "grace");

        mvc.perform(put("/api/profiles/me").with(asUser(ada))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"No Good\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ProblemType.BASE + "username-invalid"));

        mvc.perform(put("/api/profiles/me").with(asUser(ada))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"grace\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ProblemType.BASE + "username-taken"));
    }

    @Test
    void aRenameFreesTheOldHandleSoTheOldLink404s() throws Exception {
        var ada = UUID.randomUUID().toString();
        onboard(ada, "ada");

        mvc.perform(put("/api/profiles/me").with(asUser(ada))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ada_lovelace\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/profiles/ada"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(ProblemType.BASE + "profile-not-found"));
        mvc.perform(get("/api/profiles/ada_lovelace"))
                .andExpect(status().isOk());
    }
}
