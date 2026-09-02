package me.imshy.pictogram.scenario;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import javax.imageio.ImageIO;
import me.imshy.pictogram.AppOAuthWebIntegrationTest;
import me.imshy.pictogram.SharedGoogle;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.ObjectMapper;

/**
 * Driver plumbing for the {@code scenario/*} journeys — a live port, an {@link ObjectMapper}, and an
 * {@link InProcessDriver} bound to the shared Google. Boot config, the OAuth client, and DB
 * truncation all come from {@link AppOAuthWebIntegrationTest}.
 */
@AppOAuthWebIntegrationTest
public abstract class ScenarioTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper json;

    protected PictogramApi pictogram;

    @BeforeEach
    void buildDriver() {
        pictogram = new InProcessDriver(URI.create("http://localhost:" + port), SharedGoogle.INSTANCE, json);
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
}
