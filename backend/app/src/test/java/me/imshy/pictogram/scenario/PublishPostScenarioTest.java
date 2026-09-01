package me.imshy.pictogram.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import me.imshy.pictogram.scenario.PictogramApi.Post;
import me.imshy.pictogram.testsupport.SharedMinio;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The ticket's broad scenario: a user uploads a photo, publishes it with a caption, and
 * sees it on their profile. Written once against {@link PictogramApi}; green here through
 * {@link InProcessDriver}, over real HTTP, Testcontainers Postgres and MinIO. Newest-first
 * ordering across many posts is {@code PostGridTest}'s job (it controls the clock).
 */
class PublishPostScenarioTest extends ScenarioTest {

    @DynamicPropertySource
    static void objectStorage(DynamicPropertyRegistry registry) {
        SharedMinio.registerTo(registry);
    }

    @Test
    void aUserUploadsAPhotoPublishesItWithACaptionAndSeesItOnTheirProfile() {
        var ada = pictogram.registerViaGoogle("ada@example.com");
        var profile = ada.completeOnboarding("ada_lovelace", "Ada Lovelace", null);

        String mediaId = ada.uploadPhoto(jpeg());
        Post post = ada.publishPost(mediaId, "first light over the bay");

        assertThat(post.caption()).isEqualTo("first light over the bay");
        assertThat(post.mediaId()).isEqualTo(mediaId);
        assertThat(post.authorId()).isEqualTo(profile.userId());

        assertThat(ada.postsOf(profile.userId()))
                .singleElement()
                .satisfies(onGrid -> {
                    assertThat(onGrid.postId()).isEqualTo(post.postId());
                    assertThat(onGrid.caption()).isEqualTo("first light over the bay");
                });
    }

    private static byte[] jpeg() {
        try {
            var image = new BufferedImage(1200, 800, BufferedImage.TYPE_INT_RGB);
            var g = image.createGraphics();
            g.setColor(new Color(0x33, 0x66, 0x99));
            g.fillRect(0, 0, 1200, 800);
            g.dispose();
            var out = new ByteArrayOutputStream();
            ImageIO.write(image, "jpeg", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
