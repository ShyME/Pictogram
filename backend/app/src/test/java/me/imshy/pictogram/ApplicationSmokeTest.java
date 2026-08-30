package me.imshy.pictogram;

import static org.assertj.core.api.Assertions.assertThat;

import me.imshy.pictogram.testsupport.SharedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

/**
 * The whole application boots against a real PostgreSQL and serves its health probes.
 */
@SpringBootTest(
        classes = PictogramApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApplicationSmokeTest {

    @LocalServerPort
    int port;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        SharedPostgres.registerTo(registry);
    }

    @Test
    void livenessAndReadinessProbesReportUp() {
        var client = RestClient.create("http://localhost:" + port);

        assertThat(client.get().uri("/actuator/health/liveness").retrieve()
                .toBodilessEntity().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(client.get().uri("/actuator/health/readiness").retrieve()
                .toBodilessEntity().getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
