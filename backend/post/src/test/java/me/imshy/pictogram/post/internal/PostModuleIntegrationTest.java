package me.imshy.pictogram.post.internal;

import me.imshy.pictogram.media.MediaCatalog;
import me.imshy.pictogram.testsupport.ModuleIntegrationTest;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Base for post's module-integration tests — declared here so Spring Modulith bootstraps
 * the {@code post} module. {@link MediaCatalog} is media's published interface; a standalone
 * module test does not start media, so it is a mock the test programs per case (post depends
 * only on "does this media exist and who owns it", nothing about the bytes).
 */
@ApplicationModuleTest
abstract class PostModuleIntegrationTest extends ModuleIntegrationTest {

    @MockitoBean
    MediaCatalog media;
}
