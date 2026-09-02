package me.imshy.pictogram.scenario;

import java.net.URI;
import me.imshy.pictogram.AppOAuthWebIntegrationTest;
import me.imshy.pictogram.SharedGoogle;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.ObjectMapper;

/**
 * In-process transport for the {@code scenario/*} journeys: a full boot on a live port with an
 * {@link InProcessDriver} bound to the shared {@code mock-oauth2-server}. Boot config, the OAuth
 * client, and DB truncation come from {@link AppOAuthWebIntegrationTest}. The container transport is
 * {@code BlackboxScenarioTest}; the scenarios themselves live in the {@code *Scenarios} mixins.
 */
@AppOAuthWebIntegrationTest
public abstract class ScenarioTest implements PictogramScenario {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper json;

    private PictogramApi pictogram;

    @BeforeEach
    void buildDriver() {
        pictogram = new InProcessDriver(URI.create("http://localhost:" + port), SharedGoogle.INSTANCE, json);
    }

    @Override
    public PictogramApi pictogram() {
        return pictogram;
    }
}
