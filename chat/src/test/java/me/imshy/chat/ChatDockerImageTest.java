package me.imshy.chat;

import java.nio.file.Path;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

@Tag("blackbox")
class ChatDockerImageTest {

    @Test
    void builtImageAnswersItsHealthEndpoint() {
        var image = new ImageFromDockerfile().withFileFromPath(".", Path.of("."));

        // A syntactically valid EC public JWK is enough to boot chat (ADR-0014) —
        // mirrors
        // the public half of compose.yaml's fixed dev signing key.
        try (var chat = new GenericContainer<>(image).withExposedPorts(8081)
            .withEnv("PICTOGRAM_AUTH_PUBLIC_KEY",
                "{\"kty\":\"EC\",\"crv\":\"P-256\",\"kid\":\"pictogram-compose-dev\","
                    + "\"x\":\"7EWxh26yV1MJB79bw4ltkPt9iBmiewFFvBwG-DDMOTE\","
                    + "\"y\":\"jfek6RiC-fb6vxojc9T4n-QdAjwhHYoMychxZ3gSa5g\"}")
            .waitingFor(Wait.forHttp("/actuator/health").forStatusCode(200))) {
            chat.start();

            WebTestClient.bindToServer().baseUrl("http://%s:%d".formatted(chat.getHost(), chat.getMappedPort(8081)))
                .build().get().uri("/actuator/health").exchange().expectStatus().isOk();
        }
    }
}
