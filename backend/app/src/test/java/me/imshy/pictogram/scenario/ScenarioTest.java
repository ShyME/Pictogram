package me.imshy.pictogram.scenario;

import java.net.URI;
import javax.sql.DataSource;
import me.imshy.pictogram.PictogramApplication;
import me.imshy.pictogram.testsupport.DatabaseCleaner;
import me.imshy.pictogram.testsupport.SharedPostgres;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.ObjectMapper;

/**
 * Base for a {@link PictogramApi} scenario run through {@link InProcessDriver} (ADR-0007):
 * the whole application on a random port, the singleton Testcontainers Postgres, and
 * {@code mock-oauth2-server} wired in as Google. Tagged {@code fast} — it runs every build.
 */
@SpringBootTest(classes = PictogramApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("fast")
public abstract class ScenarioTest {

    // One per test JVM, started once and never stopped — reaped at shutdown, like
    // SharedPostgres. A per-class @AfterAll shutdown would break the next scenario class
    // that extends this base.
    protected static final MockOAuth2Server GOOGLE = new MockOAuth2Server();

    static {
        GOOGLE.start();
    }

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private DataSource dataSource;

    protected PictogramApi pictogram;

    @BeforeEach
    void buildDriver() {
        pictogram = new InProcessDriver(URI.create("http://localhost:" + port), GOOGLE, json);
    }

    @AfterEach
    void truncateAllTables() {
        new DatabaseCleaner(dataSource).truncateAll();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SharedPostgres.registerTo(registry);
        registry.add("spring.security.oauth2.client.registration.google.client-id", () -> InProcessDriver.CLIENT_ID);
        registry.add("spring.security.oauth2.client.registration.google.client-secret",
                () -> InProcessDriver.CLIENT_SECRET);
        registry.add("spring.security.oauth2.client.registration.google.scope", () -> "openid,email");
        registry.add("spring.security.oauth2.client.provider.google.issuer-uri",
                () -> GOOGLE.issuerUrl(InProcessDriver.ISSUER_ID).toString());
        registry.add("pictogram.auth.cookie-secure", () -> false);
    }
}
