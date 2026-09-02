package me.imshy.pictogram.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import me.imshy.pictogram.scenario.PictogramApi.DeleteOutcome;
import me.imshy.pictogram.scenario.PictogramApi.Post;
import org.junit.jupiter.api.Test;

interface DeletePostScenarios extends PictogramScenario {

    @Test
    default void anAuthorDeletesAPostAndItLeavesTheirGridEveryViewersReadOfItAndAFollowersFeed() {
        var ada = pictogram().registerViaGoogle("ada@example.com");
        var adaProfile = ada.completeOnboarding("ada_lovelace", "Ada Lovelace", null);
        var grace = pictogram().registerViaGoogle("grace@example.com");
        grace.completeOnboarding("grace_hopper", "Grace Hopper", null);
        grace.follow(adaProfile.userId());

        Post keep = ada.publishPost(ada.uploadPhoto(jpegPhoto()), "the one that stays");
        Post drop = ada.publishPost(ada.uploadPhoto(jpegPhoto()), "the one that goes");

        assertThat(ada.deletePost(drop.postId())).isEqualTo(DeleteOutcome.DELETED);

        assertThat(ada.postsOf(adaProfile.userId())).extracting(Post::postId).containsExactly(keep.postId());
        assertThat(grace.postsOf(adaProfile.userId())).extracting(Post::postId).containsExactly(keep.postId());
        assertThat(grace.openFeed().postIds()).containsExactly(keep.postId());
    }

    @Test
    default void onlyTheAuthorCanDeleteAPost() {
        var ada = pictogram().registerViaGoogle("ada@example.com");
        var adaProfile = ada.completeOnboarding("ada_lovelace", "Ada Lovelace", null);
        var grace = pictogram().registerViaGoogle("grace@example.com");
        grace.completeOnboarding("grace_hopper", "Grace Hopper", null);

        Post post = ada.publishPost(ada.uploadPhoto(jpegPhoto()), "hands off");

        assertThat(grace.deletePost(post.postId())).isEqualTo(DeleteOutcome.FORBIDDEN);

        assertThat(ada.postsOf(adaProfile.userId())).extracting(Post::postId).containsExactly(post.postId());
    }
}
