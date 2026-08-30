package me.imshy.pictogram.profile.internal;

import me.imshy.pictogram.testsupport.ModuleIntegrationTest;
import org.springframework.modulith.test.ApplicationModuleTest;

/**
 * Base for profile's module-integration tests. Declared here, in the module's own package,
 * so Spring Modulith bootstraps the {@code profile} module (it reads the module from the
 * class that declares {@code @ApplicationModuleTest}).
 */
@ApplicationModuleTest
abstract class ProfileModuleIntegrationTest extends ModuleIntegrationTest {
}
