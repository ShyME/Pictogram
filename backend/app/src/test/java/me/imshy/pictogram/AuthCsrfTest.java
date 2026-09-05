package me.imshy.pictogram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@AppIntegrationTest
class AuthCsrfTest {

    @Autowired
    MockMvc mvc;

    @Test
    void refreshWithoutACsrfTokenIsForbidden() throws Exception {
        mvc.perform(post("/api/auth/refresh")).andExpect(status().isForbidden());
    }

    @Test
    void logoutWithoutACsrfTokenIsForbidden() throws Exception {
        mvc.perform(post("/api/auth/logout")).andExpect(status().isForbidden());
    }

    @Test
    void refreshWithAHeaderThatDoesNotMatchTheCookieIsForbidden() throws Exception {
        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("XSRF-TOKEN", "the-real-token")).header("X-XSRF-TOKEN",
            "a-different-token")).andExpect(status().isForbidden());
    }

    @Test
    void theRejectedRequestSeedsAReadableXsrfTokenCookieForTheClientToEcho() throws Exception {
        var setCookie = mvc.perform(post("/api/auth/refresh")).andExpect(status().isForbidden()).andReturn()
            .getResponse().getHeaders("Set-Cookie");

        assertThat(setCookie).anySatisfy(header -> assertThat(header).startsWith("XSRF-TOKEN=")
            .doesNotContain("XSRF-TOKEN=;").doesNotContain("HttpOnly"));
    }
}
