package me.imshy.pictogram.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import me.imshy.pictogram.scenario.PictogramApi.DeleteOutcome;
import me.imshy.pictogram.scenario.PictogramApi.Post;
import me.imshy.pictogram.testsupport.SharedMinio;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

class DeletePostScenarioTest extends ScenarioTest {

    @DynamicPropertySource
    static void objectStorage(DynamicPropertyRegistry registry) {
        SharedMinio.registerTo(registry);
    }

    @Test
    void anAuthorDeletesAPostAndItLeavesTheirGridAndEveryViewersReadOfIt() {
        var ada = pictogram.registerViaGoogle("ada@example.com");
        var adaProfile = ada.completeOnboarding("ada_lovelace", "Ada Lovelace", null);
        var grace = pictogram.registerViaGoogle("grace@example.com");
        grace.completeOnboarding("grace_hopper", "Grace Hopper", null);

        Post keep = ada.publishPost(ada.uploadPhoto(jpegPhoto()), "the one that stays");
        Post drop = ada.publishPost(ada.uploadPhoto(jpegPhoto()), "the one that goes");

        assertThat(ada.deletePost(drop.postId())).isEqualTo(DeleteOutcome.DELETED);

        assertThat(ada.postsOf(adaProfile.userId()))
                .extracting(Post::postId)
                .containsExactly(keep.postId());
        assertThat(grace.postsOf(adaProfile.userId()))
                .extracting(Post::postId)
                .containsExactly(keep.postId());
    }

    @Test
    void onlyTheAuthorCanDeleteAPost() {
        var ada = pictogram.registerViaGoogle("ada@example.com");
        var adaProfile = ada.completeOnboarding("ada_lovelace", "Ada Lovelace", null);
        var grace = pictogram.registerViaGoogle("grace@example.com");
        grace.completeOnboarding("grace_hopper", "Grace Hopper", null);

        Post post = ada.publishPost(ada.uploadPhoto(jpegPhoto()), "hands off");

        assertThat(grace.deletePost(post.postId())).isEqualTo(DeleteOutcome.FORBIDDEN);

        assertThat(ada.postsOf(adaProfile.userId()))
                .extracting(Post::postId)
                .containsExactly(post.postId());
    }
}
