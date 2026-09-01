package me.imshy.pictogram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import me.imshy.pictogram.testsupport.SharedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(classes = PictogramApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OpenApiContractTest {

    private static final Path SPEC_FILE = Path.of(System.getProperty("pictogram.openapi.file", "../openapi.json"));

    private static final JsonMapper CANONICAL = JsonMapper.builder()
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        SharedPostgres.registerTo(registry);
    }

    @Test
    void committedSpecMatchesTheRunningApp() throws IOException {
        String published = canonicalize(RestClient.create("http://localhost:" + port)
                .get()
                .uri("/v3/api-docs")
                .retrieve()
                .body(String.class));

        if (Boolean.getBoolean("pictogram.openapi.generate")) {
            Files.writeString(SPEC_FILE, published, StandardCharsets.UTF_8);
            return;
        }

        if (!Files.exists(SPEC_FILE)) {
            fail("%s does not exist. Run ./gradlew :app:generateOpenApiSpec and commit it.", SPEC_FILE);
        }
        assertThat(Files.readString(SPEC_FILE, StandardCharsets.UTF_8))
                .describedAs("%s is stale. Run ./gradlew :app:generateOpenApiSpec and commit it.", SPEC_FILE)
                .isEqualTo(published);
    }

    private static String canonicalize(String json) {
        return CANONICAL.writeValueAsString(CANONICAL.readValue(json, Object.class)) + "\n";
    }
}
