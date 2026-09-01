package me.imshy.pictogram.scenario;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import javax.imageio.ImageIO;
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

@SpringBootTest(classes = PictogramApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("fast")
public abstract class ScenarioTest {

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

    protected static byte[] jpegPhoto() {
        try {
            var image = new BufferedImage(1200, 800, BufferedImage.TYPE_INT_RGB);
            var g = image.createGraphics();
            g.setColor(new Color(0x33, 0x66, 0x99));
            g.fillRect(0, 0, 1200, 800);
            g.dispose();
            var out = new ByteArrayOutputStream();
            ImageIO.write(image, "jpeg", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SharedPostgres.registerTo(registry);
        registry.add("spring.security.oauth2.client.registration.google.client-id", () -> InProcessDriver.CLIENT_ID);
        registry.add(
                "spring.security.oauth2.client.registration.google.client-secret", () -> InProcessDriver.CLIENT_SECRET);
        registry.add("spring.security.oauth2.client.registration.google.scope", () -> "openid,email");
        registry.add(
                "spring.security.oauth2.client.provider.google.issuer-uri",
                () -> GOOGLE.issuerUrl(InProcessDriver.ISSUER_ID).toString());
        registry.add("pictogram.auth.cookie-secure", () -> false);
    }
}
