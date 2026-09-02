package me.imshy.pictogram;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * The {@code local} profile exposes every actuator endpoint in full detail (application-local.yml).
 * That is safe only while the app is reachable from the developer's machine alone, so refuse to
 * start if {@code local} is active on an explicit non-loopback bind address. Runs as a web-server
 * factory customizer so the check fails the context before the listen socket is ever opened.
 */
@Component
class LocalProfileBindGuard implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {

    private final Environment environment;

    LocalProfileBindGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        verify(environment.matchesProfiles("local"), environment.getProperty("server.address"));
    }

    static void verify(boolean localProfileActive, String serverAddress) {
        if (!localProfileActive || !StringUtils.hasText(serverAddress)) {
            return;
        }
        InetAddress address;
        try {
            address = InetAddress.getByName(serverAddress);
        } catch (UnknownHostException e) {
            throw new IllegalStateException("server.address '" + serverAddress + "' could not be resolved", e);
        }
        if (!address.isLoopbackAddress()) {
            throw new IllegalStateException("The 'local' profile exposes every actuator endpoint in full detail and "
                    + "must bind to a loopback address; server.address '" + serverAddress + "' is not loopback. "
                    + "Drop the 'local' profile for any non-local bind.");
        }
    }
}
