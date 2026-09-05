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

        try (var chat = new GenericContainer<>(image).withExposedPorts(8081)
            .waitingFor(Wait.forHttp("/actuator/health").forStatusCode(200))) {
            chat.start();

            WebTestClient.bindToServer().baseUrl("http://%s:%d".formatted(chat.getHost(), chat.getMappedPort(8081)))
                .build().get().uri("/actuator/health").exchange().expectStatus().isOk();
        }
    }
}
