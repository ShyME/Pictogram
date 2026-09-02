package me.imshy.pictogram.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import me.imshy.pictogram.scenario.PictogramApi.Post;
import org.junit.jupiter.api.Test;

interface PublishPostScenarios extends PictogramScenario {

    @Test
    default void aUserUploadsAPhotoPublishesItWithACaptionAndSeesItOnTheirProfile() {
        var ada = pictogram().registerViaGoogle("ada@example.com");
        var profile = ada.completeOnboarding("ada_lovelace", "Ada Lovelace", null);

        String mediaId = ada.uploadPhoto(jpegPhoto());
        Post post = ada.publishPost(mediaId, "first light over the bay");

        assertThat(post.caption()).isEqualTo("first light over the bay");
        assertThat(post.mediaId()).isEqualTo(mediaId);
        assertThat(post.authorId()).isEqualTo(profile.userId());

        assertThat(ada.postsOf(profile.userId())).singleElement().satisfies(onGrid -> {
            assertThat(onGrid.postId()).isEqualTo(post.postId());
            assertThat(onGrid.caption()).isEqualTo("first light over the bay");
        });
    }
}
