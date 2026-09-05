package me.imshy.pictogram.scenario;

import java.net.URI;
import me.imshy.pictogram.AppOAuthWebIntegrationTest;
import me.imshy.pictogram.SharedGoogle;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.ObjectMapper;

@AppOAuthWebIntegrationTest
public abstract class AppIntegrationTest implements AppUnderTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper json;

    private PictogramApp pictogramApp;

    @BeforeEach
    void connectToTheApp() {
        pictogramApp = new SpringBootApp(URI.create("http://localhost:" + port), SharedGoogle.INSTANCE, json);
    }

    @Override
    public PictogramApp pictogram() {
        return pictogramApp;
    }
}
