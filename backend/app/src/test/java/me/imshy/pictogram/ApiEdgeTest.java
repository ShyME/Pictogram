package me.imshy.pictogram;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.shared.http.ApiException;
import me.imshy.pictogram.shared.http.ApiPage;
import me.imshy.pictogram.shared.http.CurrentUser;
import me.imshy.pictogram.shared.http.Cursor;
import me.imshy.pictogram.shared.http.ProblemType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AppIntegrationTest
@Import(ApiEdgeTest.ProbeController.class)
class ApiEdgeTest {

    @Autowired
    MockMvc mvc;

    @Test
    void missingAccessTokenIsA401ProblemDetail() throws Exception {
        mvc.perform(get("/api/_probe/current-user"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(
                        jsonPath("$.type").value(ProblemType.UNAUTHORIZED.uri().toString()))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Authentication required"))
                .andExpect(jsonPath("$.properties").doesNotExist());
    }

    @Test
    void invalidAccessTokenIsA401ProblemDetail() throws Exception {
        mvc.perform(get("/api/_probe/current-user").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(
                        jsonPath("$.type").value(ProblemType.UNAUTHORIZED.uri().toString()));
    }

    @Test
    void currentUserIsResolvedFromTheAccessTokenSubject() throws Exception {
        var user = UUID.randomUUID();

        mvc.perform(get("/api/_probe/current-user").with(jwt().jwt(jwt -> jwt.subject(user.toString()))))
                .andExpect(status().isOk())
                .andExpect(content().string("\"" + user + "\""));
    }

    @Test
    void currentViewerIsResolvedFromTheSameSubject() throws Exception {
        var user = UUID.randomUUID();

        mvc.perform(get("/api/_probe/current-viewer").with(jwt().jwt(jwt -> jwt.subject(user.toString()))))
                .andExpect(status().isOk())
                .andExpect(content().string("\"" + user + "\""));
    }

    @Test
    void listEndpointsReturnAnEnvelopeWhoseCursorIsAnOpaqueDecodableToken() throws Exception {
        var expectedCursor = new Cursor(Instant.parse("2026-08-30T12:00:00Z"), ProbeController.PAGE_TAIL);

        mvc.perform(get("/api/_probe/page").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0]").value("first"))
                .andExpect(jsonPath("$.nextCursor").value(expectedCursor.encode()))
                .andExpect(jsonPath("$.nextCursor").value(not(containsString("2026"))));
    }

    @Test
    void anApiExceptionRendersAsAProblemDetailWithItsStableType() throws Exception {
        mvc.perform(get("/api/_probe/boom").with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ProblemType.BASE + "probe-conflict"))
                .andExpect(jsonPath("$.detail").value("the probe blew up"));
    }

    @Test
    void aMalformedCursorRendersAsA400ProblemDetail() throws Exception {
        mvc.perform(get("/api/_probe/by-cursor")
                        .param("cursor", "!!not-a-cursor!!")
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type")
                        .value(ProblemType.INVALID_CURSOR.uri().toString()));
    }

    @Test
    void aFrameworkErrorStillRendersAsProblemJson() throws Exception {
        mvc.perform(get("/api/_probe/no-such-route").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void timestampsSerializeAsIso8601Utc() throws Exception {
        mvc.perform(get("/api/_probe/clock").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.at").value("2026-08-30T12:00:00Z"));
    }

    @RestController
    @RequestMapping("/api/_probe")
    static class ProbeController {

        static final UUID PAGE_TAIL = UUID.fromString("00000000-0000-0000-0000-0000000000ff");

        record Clock(Instant at) {}

        @GetMapping("/current-user")
        UserId currentUser(@CurrentUser UserId user) {
            return user;
        }

        @GetMapping("/current-viewer")
        ViewerId currentViewer(@CurrentUser ViewerId viewer) {
            return viewer;
        }

        @GetMapping("/page")
        ApiPage<String> page() {
            return ApiPage.of(List.of("first"), new Cursor(Instant.parse("2026-08-30T12:00:00Z"), PAGE_TAIL));
        }

        @GetMapping("/boom")
        void boom() {
            throw new ApiException(
                    HttpStatus.CONFLICT, new ProblemType("probe-conflict", "Probe conflict"), "the probe blew up");
        }

        @GetMapping("/by-cursor")
        ApiPage<String> byCursor(@RequestParam String cursor) {
            Cursor.decode(cursor);
            return ApiPage.lastPage(List.of());
        }

        @GetMapping("/clock")
        Clock clock() {
            return new Clock(Instant.parse("2026-08-30T12:00:00Z"));
        }
    }
}
