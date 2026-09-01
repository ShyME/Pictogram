package me.imshy.pictogram.post.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import me.imshy.pictogram.post.PostDeleted;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.http.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.AssertablePublishedEvents;

/**
 * The delete command from the ticket: an author permanently removes one of their own posts,
 * it disappears from their grid, and the delete announces {@link PostDeleted}. Only the
 * author may delete — anyone else is a {@link ForbiddenException} and the post is left
 * standing. Deleting a post that isn't there is a {@link PostNotFoundException}.
 */
class PostDeletionTest extends PostModuleIntegrationTest {

    @Autowired
    Publishing publishing;

    @Autowired
    PostDeletion deletion;

    @Autowired
    PostTimeline timeline;

    @Test
    void deletesTheAuthorsOwnPostAndItLeavesTheirGrid(AssertablePublishedEvents events) {
        var author = UserId.random();
        var mediaId = MediaId.random();
        given(media.ownerOf(mediaId)).willReturn(Optional.of(author));
        PostView post = publishing.publish(author, mediaId, "goodbye");

        deletion.delete(author, post.postId());

        assertThat(timeline.pageFor(author, null, null).items()).isEmpty();
        assertThat(events.ofType(PostDeleted.class))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.postId()).isEqualTo(post.postId());
                    assertThat(event.authorId()).isEqualTo(author);
                    assertThat(event.mediaId()).isEqualTo(mediaId);
                    assertThat(event.deletedAt()).isNotNull();
                });
    }

    @Test
    void refusesToDeleteAPostTheCallerDidNotWriteAndLeavesItStanding(AssertablePublishedEvents events) {
        var author = UserId.random();
        var interloper = UserId.random();
        var mediaId = MediaId.random();
        given(media.ownerOf(mediaId)).willReturn(Optional.of(author));
        PostView post = publishing.publish(author, mediaId, null);

        assertThatExceptionOfType(ForbiddenException.class)
                .isThrownBy(() -> deletion.delete(interloper, post.postId()));

        assertThat(timeline.pageFor(author, null, null).items())
                .extracting(PostView::postId)
                .containsExactly(post.postId());
        assertThat(events.ofType(PostDeleted.class)).isEmpty();
    }

    @Test
    void rejectsAPostIdNoPostHas() {
        assertThatExceptionOfType(PostNotFoundException.class)
                .isThrownBy(() -> deletion.delete(UserId.random(), PostId.random()));
    }

    @Test
    void deletingOnePostLeavesTheAuthorsOtherPostsAlone() {
        var author = UserId.random();
        var keep = MediaId.random();
        var drop = MediaId.random();
        given(media.ownerOf(keep)).willReturn(Optional.of(author));
        given(media.ownerOf(drop)).willReturn(Optional.of(author));
        PostId kept = publishing.publish(author, keep, null).postId();
        PostId dropped = publishing.publish(author, drop, null).postId();

        deletion.delete(author, dropped);

        assertThat(timeline.pageFor(author, null, null).items())
                .extracting(PostView::postId)
                .containsExactly(kept);
    }
}
