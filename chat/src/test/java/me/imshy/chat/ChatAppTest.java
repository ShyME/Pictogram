package me.imshy.chat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

// A syntactically valid EC public JWK is enough to boot the context (ADR-0014's
// verifier needs *a* key, not a real one, for a health-endpoint-only test) — mirrors the
// public half of compose.yaml's fixed dev signing key.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "pictogram.auth.public-key={\"kty\":\"EC\",\"crv\":\"P-256\",\"kid\":\"pictogram-compose-dev\","
        + "\"x\":\"7EWxh26yV1MJB79bw4ltkPt9iBmiewFFvBwG-DDMOTE\","
        + "\"y\":\"jfek6RiC-fb6vxojc9T4n-QdAjwhHYoMychxZ3gSa5g\"}")
class ChatAppTest {

    @LocalServerPort
    private int port;

    @Test
    void healthEndpointRespondsOk() {
        WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build().get().uri("/actuator/health")
            .exchange().expectStatus().isEqualTo(HttpStatus.OK);
    }
}
