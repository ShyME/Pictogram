package me.imshy.pictogram.media.internal;

import me.imshy.pictogram.testsupport.ModuleIntegrationTest;
import me.imshy.pictogram.testsupport.SharedMinio;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base for media's module-integration tests — declared here so Spring Modulith bootstraps
 * the {@code media} module. Adds the singleton {@link SharedMinio} to the Testcontainers
 * Postgres the parent already wires, so a test exercises the real re-encode, the real S3
 * client, and the real repository together.
 */
@ApplicationModuleTest
abstract class MediaModuleIntegrationTest extends ModuleIntegrationTest {

    @DynamicPropertySource
    static void objectStorage(DynamicPropertyRegistry registry) {
        SharedMinio.registerTo(registry);
    }
}
