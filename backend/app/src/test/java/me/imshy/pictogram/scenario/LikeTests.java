package me.imshy.pictogram.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import me.imshy.pictogram.scenario.PictogramApp.PostLikes;
import org.junit.jupiter.api.Test;

interface LikeTests extends AppUnderTest {

    @Test
    default void aViewerLikesAndUnlikesAPostWithIdempotentRepeats() {
        var ada = pictogram().registerViaGoogle("ada@example.com");
        ada.completeOnboarding("ada_likes", "Ada", null);
        var bob = pictogram().registerViaGoogle("bob@example.com");
        bob.completeOnboarding("bob_likes", "Bob", null);
        String post = bob.publishPost(bob.uploadPhoto(JpegPhoto.some()), "a photo").postId();

        assertThat(likeState(ada, post)).isEqualTo(new PostLikes(0, false));

        ada.like(post);
        ada.like(post);
        assertThat(likeState(ada, post)).isEqualTo(new PostLikes(1, true));
        assertThat(likeState(bob, post)).isEqualTo(new PostLikes(1, false));

        ada.unlike(post);
        ada.unlike(post);
        assertThat(likeState(ada, post)).isEqualTo(new PostLikes(0, false));
    }

    @Test
    default void aViewerMayLikeTheirOwnPost() {
        var ada = pictogram().registerViaGoogle("ada@example.com");
        ada.completeOnboarding("ada_selflike", "Ada", null);
        String ownPost = ada.publishPost(ada.uploadPhoto(JpegPhoto.some()), "mine").postId();

        ada.like(ownPost);

        assertThat(likeState(ada, ownPost)).isEqualTo(new PostLikes(1, true));
    }

    private static PostLikes likeState(PictogramApp.PictogramApi viewer, String postId) {
        return viewer.likesOf(postId).get(postId);
    }
}
