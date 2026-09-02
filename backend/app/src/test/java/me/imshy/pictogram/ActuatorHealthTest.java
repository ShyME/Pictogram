package me.imshy.pictogram;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@code /actuator/health} is the readiness signal the Playwright global setup polls
 * ({@code frontend/e2e/globalSetup.ts}): it must be reachable without authentication and report
 * {@code "status":"UP"} once the app is serving.
 */
@AppIntegrationTest
class ActuatorHealthTest {

    @Autowired
    MockMvc mvc;

    @Test
    void healthIsPublicAndReportsUpWithNoAuth() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"status\":\"UP\"")))
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
