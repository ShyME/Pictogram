package me.imshy.pictogram.follow.internal;

import me.imshy.pictogram.testsupport.ModuleIntegrationTest;
import org.springframework.modulith.test.ApplicationModuleTest;

/**
 * Base for follow's module-integration tests. Declared here, in the module's own package,
 * so Spring Modulith bootstraps the {@code follow} module (it reads the module from the
 * class that declares {@code @ApplicationModuleTest}). follow depends on nothing but
 * {@code shared-kernel}, so there is nothing to mock.
 */
@ApplicationModuleTest
abstract class FollowModuleIntegrationTest extends ModuleIntegrationTest {
}
