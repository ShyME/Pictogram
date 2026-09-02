package me.imshy.pictogram;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * The {@code local} profile exposes every actuator endpoint in full detail (application-local.yml).
 * That is safe only while the app is reachable from the developer's machine alone, so refuse to
 * finish starting if {@code local} is active on an explicit non-loopback bind address.
 */
@Component
class LocalProfileBindGuard implements ApplicationListener<ApplicationStartedEvent> {

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        Environment environment = event.getApplicationContext().getEnvironment();
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
