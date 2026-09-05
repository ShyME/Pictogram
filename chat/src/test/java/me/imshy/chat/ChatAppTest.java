package me.imshy.chat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatAppTest {

    @LocalServerPort
    private int port;

    @Test
    void healthEndpointRespondsOk() {
        WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build().get().uri("/actuator/health")
            .exchange().expectStatus().isEqualTo(HttpStatus.OK);
    }
}
