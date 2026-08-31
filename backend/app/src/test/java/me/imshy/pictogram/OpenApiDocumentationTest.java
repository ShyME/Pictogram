package me.imshy.pictogram;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The committed {@code openapi.json} must describe the SPA-facing auth and profile endpoints
 * as they actually behave — the real status codes, the {@code Location} header, and the
 * Problem Detail error bodies — so the generated TypeScript client carries them instead of
 * the frontend hand-declaring response shapes and branching on raw status numbers (#35).
 *
 * <p>Reads the file directly rather than booting the app: {@link OpenApiContractTest}
 * already guarantees the file equals what the running app publishes.
 */
class OpenApiDocumentationTest {

    private static JsonNode spec;

    @BeforeAll
    static void readSpec() throws IOException {
        Path file = Path.of(System.getProperty("pictogram.openapi.file", "../openapi.json"));
        spec = JsonMapper.builder().build().readTree(Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void onboardingIsDocumentedAs201WithLocationAndProblemDetailErrors() {
        JsonNode onboard = spec.at("/paths/~1api~1profiles/post/responses");

        assertThat(onboard.has("200")).as("no phantom 200").isFalse();
        assertThat(onboard.at("/201/headers/Location")).isNotEmpty();
        assertThat(onboard.at("/201/content/application~1json/schema/$ref").asString())
                .endsWith("/ProfileView");
        assertThat(onboard.at("/400/content/application~1problem+json/schema/$ref").asString())
                .endsWith("/ProblemDetail");
        assertThat(onboard.at("/409/content/application~1problem+json/schema/$ref").asString())
                .endsWith("/ProblemDetail");
    }

    @Test
    void ownProfileIsDocumentedWith200And404() {
        JsonNode me = spec.at("/paths/~1api~1profiles~1me/get/responses");

        assertThat(me.at("/200/content/application~1json/schema/$ref").asString()).endsWith("/ProfileView");
        assertThat(me.at("/404/content/application~1problem+json/schema/$ref").asString())
                .endsWith("/ProblemDetail");
    }

    @Test
    void refreshIsDocumentedWithATypedBodyAndA401() {
        JsonNode refresh = spec.at("/paths/~1api~1auth~1refresh/post/responses");

        String okRef = refresh.at("/200/content/application~1json/schema/$ref").asString();
        assertThat(okRef).describedAs("200 body is a named schema, not type:object").endsWith("/AccessTokenResponse");
        JsonNode body = spec.at("/components/schemas/AccessTokenResponse");
        assertThat(body.at("/properties/accessToken")).isNotEmpty();
        assertThat(body.at("/properties/expiresInSeconds")).isNotEmpty();

        assertThat(refresh.at("/401/content/application~1problem+json/schema/$ref").asString())
                .endsWith("/ProblemDetail");
    }

    @Test
    void logoutIsDocumentedAs204() {
        JsonNode logout = spec.at("/paths/~1api~1auth~1logout/post/responses");

        assertThat(logout.has("204")).isTrue();
        assertThat(logout.has("200")).as("no phantom 200").isFalse();
    }
}
