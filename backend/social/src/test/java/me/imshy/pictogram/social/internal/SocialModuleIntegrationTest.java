package me.imshy.pictogram.social.internal;

import me.imshy.pictogram.post.PublishedPosts;
import me.imshy.pictogram.testsupport.ModuleIntegrationTest;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApplicationModuleTest
public abstract class SocialModuleIntegrationTest extends ModuleIntegrationTest {

    @MockitoBean
    PublishedPosts publishedPosts;
}
