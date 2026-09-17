package me.imshy.pictogram;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

// ADR-0016: /actuator/prometheus is public within the app context (Caddyfile.prod keeps it
// off the public domain — see the Caddyfile comment, not application-security here).
@AppIntegrationTest
class PrometheusMetricsApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void exposesHttpJvmAndProductStatGauges() throws Exception {
        mvc.perform(get("/actuator/prometheus")).andExpect(status().isOk())
            .andExpect(content().string(containsString("jvm_memory_used_bytes")))
            .andExpect(content().string(containsString("pictogram_users_count")))
            .andExpect(content().string(containsString("pictogram_users_active")))
            .andExpect(content().string(containsString("pictogram_posts_count")))
            .andExpect(content().string(containsString("pictogram_comments_count")))
            .andExpect(content().string(containsString("pictogram_likes_count")))
            .andExpect(content().string(containsString("pictogram_follows_count")));
    }
}
