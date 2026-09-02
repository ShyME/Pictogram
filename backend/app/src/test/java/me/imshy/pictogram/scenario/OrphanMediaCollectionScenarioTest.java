package me.imshy.pictogram.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import me.imshy.pictogram.media.MediaCatalog;
import me.imshy.pictogram.media.OrphanCollection;
import me.imshy.pictogram.scenario.PictogramApi.Post;
import me.imshy.pictogram.shared.MediaId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

class OrphanMediaCollectionScenarioTest extends ScenarioTest {

    @Autowired
    OrphanCollection orphanCollection;

    @Autowired
    MediaCatalog media;

    @DynamicPropertySource
    static void retention(DynamicPropertyRegistry registry) {
        registry.add("pictogram.media.retention.grace-period", () -> "0s");
    }

    @Test
    void sweepsTheNeverPostedAndTheDeletedPostsImageButKeepsTheLiveOne() {
        var ada = pictogram.registerViaGoogle("ada@example.com");
        var adaProfile = ada.completeOnboarding("ada_lovelace", "Ada Lovelace", null);

        Post kept = ada.publishPost(ada.uploadPhoto(jpegPhoto()), "the one that stays");
        String neverPosted = ada.uploadPhoto(jpegPhoto());
        Post doomed = ada.publishPost(ada.uploadPhoto(jpegPhoto()), "the one that goes");
        ada.deletePost(doomed.postId());

        int collected = orphanCollection.collectOrphans();

        assertThat(collected).isEqualTo(2);
        assertThat(media.exists(mediaId(kept.mediaId()))).isTrue();
        assertThat(media.exists(mediaId(neverPosted))).isFalse();
        assertThat(media.exists(mediaId(doomed.mediaId()))).isFalse();
        assertThat(ada.postsOf(adaProfile.userId())).extracting(Post::postId).containsExactly(kept.postId());
    }

    private static MediaId mediaId(String value) {
        return MediaId.fromString(value);
    }
}
