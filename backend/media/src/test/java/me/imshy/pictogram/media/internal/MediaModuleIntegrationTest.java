package me.imshy.pictogram.media.internal;

import me.imshy.pictogram.media.PostReferences;
import me.imshy.pictogram.testsupport.ModuleIntegrationTest;
import me.imshy.pictogram.testsupport.SharedMinio;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApplicationModuleTest
abstract class MediaModuleIntegrationTest extends ModuleIntegrationTest {

    @MockitoBean
    PostReferences postReferences;

    @DynamicPropertySource
    static void objectStorage(DynamicPropertyRegistry registry) {
        SharedMinio.registerTo(registry);
    }
}
