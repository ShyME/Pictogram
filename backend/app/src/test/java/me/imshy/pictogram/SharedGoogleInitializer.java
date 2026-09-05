package me.imshy.pictogram;

import java.util.Map;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

public class SharedGoogleInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        context.getEnvironment().getPropertySources()
            .addFirst(new MapPropertySource("sharedGoogle",
                Map.of("spring.security.oauth2.client.registration.google.client-id", SharedGoogle.CLIENT_ID,
                    "spring.security.oauth2.client.registration.google.client-secret", SharedGoogle.CLIENT_SECRET,
                    "spring.security.oauth2.client.registration.google.scope", "openid,email",
                    "spring.security.oauth2.client.provider.google.issuer-uri",
                    SharedGoogle.INSTANCE.issuerUrl(SharedGoogle.ISSUER_ID).toString())));
    }
}
