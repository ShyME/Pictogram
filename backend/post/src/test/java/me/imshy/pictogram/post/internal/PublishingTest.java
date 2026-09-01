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

class PublishingTest extends PostModuleIntegrationTest {

    @Autowired
    Publishing publishing;

    @Autowired
    PostTimeline timeline;

    @Test
    void publishesAPostForMediaTheAuthorOwnsAndItLandsOnTheirGrid(AssertablePublishedEvents events) {
        var author = UserId.random();
        var mediaId = MediaId.random();
        given(media.ownerOf(mediaId)).willReturn(Optional.of(author));

        PostView post = publishing.publish(author, mediaId, "my first post");

        assertThat(post.authorId()).isEqualTo(author);
        assertThat(post.mediaId()).isEqualTo(mediaId);
        assertThat(post.caption()).isEqualTo("my first post");
        assertThat(post.publishedAt()).isNotNull();
        assertThat(timeline.pageFor(author, null, null).items())
                .extracting(PostView::postId)
                .containsExactly(post.postId());
        assertThat(events.ofType(PostPublished.class))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.postId()).isEqualTo(post.postId());
                    assertThat(event.authorId()).isEqualTo(author);
                    assertThat(event.mediaId()).isEqualTo(mediaId);
                });
    }

    @Test
    void publishesWithoutACaption() {
        var author = UserId.random();
        var mediaId = MediaId.random();
        given(media.ownerOf(mediaId)).willReturn(Optional.of(author));

        assertThat(publishing.publish(author, mediaId, null).caption()).isNull();
    }

    @Test
    void rejectsAMediaIdNoMediaHas() {
        given(media.ownerOf(any())).willReturn(Optional.empty());

        assertThatExceptionOfType(UnusableMediaException.class)
                .isThrownBy(() -> publishing.publish(UserId.random(), MediaId.random(), null));
    }

    @Test
    void rejectsMediaUploadedBySomeoneElse() {
        var mediaId = MediaId.random();
        given(media.ownerOf(mediaId)).willReturn(Optional.of(UserId.random()));

        assertThatExceptionOfType(UnusableMediaException.class)
                .isThrownBy(() -> publishing.publish(UserId.random(), mediaId, null));
    }

    @Test
    void rejectsAnOverLongCaption() {
        var author = UserId.random();
        var mediaId = MediaId.random();
        given(media.ownerOf(mediaId)).willReturn(Optional.of(author));

        assertThatExceptionOfType(CaptionTooLongException.class)
                .isThrownBy(() -> publishing.publish(author, mediaId, "x".repeat(Caption.MAX_LENGTH + 1)));
    }

    @Test
    void nothingIsPublishedWhenTheMediaIsUnusable(AssertablePublishedEvents events) {
        given(media.ownerOf(any())).willReturn(Optional.empty());
        var author = UserId.random();

        assertThatExceptionOfType(UnusableMediaException.class)
                .isThrownBy(() -> publishing.publish(author, MediaId.random(), "caption"));

        assertThat(timeline.pageFor(author, null, null).items()).isEmpty();
        assertThat(events.ofType(PostPublished.class)).isEmpty();
    }
}
