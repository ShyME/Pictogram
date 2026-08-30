package me.imshy.pictogram.identity.internal;

import me.imshy.pictogram.testsupport.ModuleIntegrationTest;
import org.springframework.modulith.test.ApplicationModuleTest;

/**
 * Base for identity's module-integration tests. Declared here, in the module's own package,
 * so Spring Modulith bootstraps the {@code identity} module (it reads the module from the
 * class that declares {@code @ApplicationModuleTest}).
 */
@ApplicationModuleTest
abstract class IdentityModuleIntegrationTest extends ModuleIntegrationTest {
}
