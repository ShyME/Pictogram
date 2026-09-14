package me.imshy.pictogram;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import me.imshy.pictogram.shared.http.ProblemType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@AppIntegrationTest
class RateLimitApiTest {

    @Autowired
    MockMvc mvc;

    @DynamicPropertySource
    static void tinyWriteLimit(DynamicPropertyRegistry registry) {
        registry.add("pictogram.ratelimit.write.capacity", () -> "2");
        registry.add("pictogram.ratelimit.write.window", () -> "1m");
    }

    @Test
    void aViewerWhoExceedsTheWritePathLimitGetsATooManyRequestsProblemDetail() throws Exception {
        var ada = UUID.randomUUID().toString();

        like(ada).andExpect(status().isNoContent());
        like(ada).andExpect(status().isNoContent());

        like(ada).andExpect(status().is(429))
            .andExpect(jsonPath("$.type").value(ProblemType.BASE + "rate-limit-exceeded"));
    }

    @Test
    void theLimitIsScopedPerUserNotGlobal() throws Exception {
        var ada = UUID.randomUUID().toString();
        var bob = UUID.randomUUID().toString();

        like(ada).andExpect(status().isNoContent());
        like(ada).andExpect(status().isNoContent());
        like(ada).andExpect(status().is(429));

        like(bob).andExpect(status().isNoContent());
    }

    private ResultActions like(String viewer) throws Exception {
        return mvc.perform(put("/api/likes/" + UUID.randomUUID()).with(jwt().jwt(jwt -> jwt.subject(viewer))));
    }
}
