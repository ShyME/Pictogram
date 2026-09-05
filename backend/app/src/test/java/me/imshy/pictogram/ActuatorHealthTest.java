package me.imshy.pictogram;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@AppIntegrationTest
class ActuatorHealthTest {

    @Autowired
    MockMvc mvc;

    @Test
    void healthIsPublicAndReportsUpWithNoAuth() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
            .andExpect(content().string(containsString("\"status\":\"UP\"")))
            .andExpect(jsonPath("$.status").value("UP"));
    }
}
