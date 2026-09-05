package me.imshy.pictogram.post.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import me.imshy.pictogram.post.PostPublished;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.AssertablePublishedEvents;

class PostPublishingTest extends PostModuleIntegrationTest {

    @Autowired
    PostPublishing postPublishing;

    @Autowired
    PostTimeline postTimeline;

    @Test
    void publishesAPostForMediaTheAuthorOwnsAndItLandsOnTheirGrid(AssertablePublishedEvents events) {
        var author = UserId.random();
        var mediaId = MediaId.random();
        given(mediaCatalog.ownerOf(mediaId)).willReturn(Optional.of(author));

        PostView post = postPublishing.publish(author, mediaId, "my first post");

        assertThat(post.authorId()).isEqualTo(author);
        assertThat(post.mediaId()).isEqualTo(mediaId);
        assertThat(post.caption()).isEqualTo("my first post");
        assertThat(post.publishedAt()).isNotNull();
        assertThat(postTimeline.pageFor(author, null, null).items()).extracting(PostView::postId)
            .containsExactly(post.postId());
        assertThat(events.ofType(PostPublished.class)).singleElement().satisfies(event -> {
            assertThat(event.postId()).isEqualTo(post.postId());
            assertThat(event.authorId()).isEqualTo(author);
            assertThat(event.mediaId()).isEqualTo(mediaId);
        });
    }

    @Test
    void publishesWithoutACaption() {
        var author = UserId.random();
        var mediaId = MediaId.random();
        given(mediaCatalog.ownerOf(mediaId)).willReturn(Optional.of(author));

        assertThat(postPublishing.publish(author, mediaId, null).caption()).isNull();
    }

    @Test
    void rejectsAMediaIdNoMediaHas() {
        given(mediaCatalog.ownerOf(any())).willReturn(Optional.empty());

        assertThatExceptionOfType(UnusableMediaException.class)
            .isThrownBy(() -> postPublishing.publish(UserId.random(), MediaId.random(), null));
    }

    @Test
    void rejectsMediaUploadedBySomeoneElse() {
        var mediaId = MediaId.random();
        given(mediaCatalog.ownerOf(mediaId)).willReturn(Optional.of(UserId.random()));

        assertThatExceptionOfType(UnusableMediaException.class)
            .isThrownBy(() -> postPublishing.publish(UserId.random(), mediaId, null));
    }

    @Test
    void rejectsAnOverLongCaption() {
        var author = UserId.random();
        var mediaId = MediaId.random();
        given(mediaCatalog.ownerOf(mediaId)).willReturn(Optional.of(author));

        assertThatExceptionOfType(CaptionTooLongException.class)
            .isThrownBy(() -> postPublishing.publish(author, mediaId, "x".repeat(Caption.MAX_LENGTH + 1)));
    }

    @Test
    void nothingIsPublishedWhenTheMediaIsUnusable(AssertablePublishedEvents events) {
        given(mediaCatalog.ownerOf(any())).willReturn(Optional.empty());
        var author = UserId.random();

        assertThatExceptionOfType(UnusableMediaException.class)
            .isThrownBy(() -> postPublishing.publish(author, MediaId.random(), "caption"));

        assertThat(postTimeline.pageFor(author, null, null).items()).isEmpty();
        assertThat(events.ofType(PostPublished.class)).isEmpty();
    }
}
