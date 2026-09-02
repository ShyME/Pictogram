package me.imshy.pictogram;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

@AppWebIntegrationTest
class ApplicationSmokeTest {

    @LocalServerPort
    int port;

    @Test
    void livenessAndReadinessProbesReportUp() {
        var client = RestClient.create("http://localhost:" + port);

        assertThat(client.get()
                        .uri("/actuator/health/liveness")
                        .retrieve()
                        .toBodilessEntity()
                        .getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(client.get()
                        .uri("/actuator/health/readiness")
                        .retrieve()
                        .toBodilessEntity()
                        .getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }
}
