package me.imshy.pictogram;

import me.imshy.pictogram.testsupport.SharedMinio;
import me.imshy.pictogram.testsupport.SharedPostgres;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;

/**
 * The one property source for every full-boot app test: Postgres, MinIO, and the auth-cookie flag,
 * all from {@link DynamicPropertyRegistrar} beans rather than scattered static
 * {@code @DynamicPropertySource} methods — so the context-cache key is byte-identical everywhere
 * this is imported (#78).
 *
 * <p>Registrar beans, not a {@code @ServiceConnection} container bean: {@link SharedPostgres#INSTANCE}
 * is a JVM-wide singleton with its own lifecycle, and a {@code @ServiceConnection} bean would put it
 * under Spring Boot's Testcontainers lifecycle — one context failing to refresh would then
 * {@code stop()} the container out from under the other cached contexts. These props are read at
 * bean-creation time, so the later timing of registrar beans is fine (unlike the OAuth client — see
 * {@link SharedGoogleInitializer}).
 */
@TestConfiguration(proxyBeanMethods = false)
class SharedWebTestConfig {

    @Bean
    DynamicPropertyRegistrar datasourceProperties() {
        return SharedPostgres::registerTo;
    }

    @Bean
    DynamicPropertyRegistrar objectStorageProperties() {
        return SharedMinio::registerTo;
    }

    @Bean
    DynamicPropertyRegistrar authCookieProperties() {
        return registry -> registry.add("pictogram.auth.cookie-secure", () -> false);
    }
}
