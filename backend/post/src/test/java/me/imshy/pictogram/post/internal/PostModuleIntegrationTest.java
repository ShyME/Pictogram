package me.imshy.pictogram.post.internal;

import me.imshy.pictogram.media.MediaCatalog;
import me.imshy.pictogram.testsupport.ModuleIntegrationTest;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApplicationModuleTest
abstract class PostModuleIntegrationTest extends ModuleIntegrationTest {

    @MockitoBean
    MediaCatalog mediaCatalog;
}
