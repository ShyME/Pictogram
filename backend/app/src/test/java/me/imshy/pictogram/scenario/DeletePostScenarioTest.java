package me.imshy.pictogram.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import me.imshy.pictogram.scenario.PictogramApi.DeleteOutcome;
import me.imshy.pictogram.scenario.PictogramApi.Post;
import me.imshy.pictogram.testsupport.SharedMinio;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The ticket's broad scenario: an author deletes one of their posts and it vanishes from
 * every grid it was on — their own, and another viewer's read of it. (Its disappearance
 * from a follower's feed is the same assertion once the feed assembles from post
 * fan-out-on-read; the feed is a single empty page until #18, which should fold that
 * assertion in here.) A second scenario pins the rule that only the author may delete:
 * anyone else is turned away and the post stands.
 */
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
