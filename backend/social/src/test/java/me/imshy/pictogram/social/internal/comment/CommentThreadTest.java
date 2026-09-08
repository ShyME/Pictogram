package me.imshy.pictogram.social.internal.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import me.imshy.pictogram.post.PostDeleted;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.shared.http.Cursor;
import me.imshy.pictogram.shared.http.ForbiddenException;
import me.imshy.pictogram.social.CommentDeleted;
import me.imshy.pictogram.social.PostCommented;
import me.imshy.pictogram.social.internal.SocialModuleIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class CommentThreadTest extends SocialModuleIntegrationTest {

    @Autowired
    CommentThread thread;

    @Autowired
    ApplicationEventPublisher events;

    @MockitoBean
    Clock clock;

    private Instant now = Instant.parse("2026-09-04T12:00:00Z");
    private final ViewerId author = ViewerId.random();
    private final ViewerId postAuthor = ViewerId.random();
    private final PostId post = PostId.random();

    @BeforeEach
    void bindClock() {
        given(clock.instant()).willAnswer(invocation -> now);
        given(publishedPosts.authorOf(post)).willReturn(Optional.of(new UserId(postAuthor.value())));
    }

    @Test
    void aCommentIsStoredAndReturnedWithItsGeneratedIdAndTimestamp(AssertablePublishedEvents publishedEvents) {
        PostComment written = thread.comment(author, post, "first!");

        assertThat(written.commentId()).isNotNull();
        assertThat(written.postId()).isEqualTo(post);
        assertThat(written.viewer()).isEqualTo(author);
        assertThat(written.body()).isEqualTo("first!");
        assertThat(written.createdAt()).isEqualTo(now);

        assertThat(thread.pageFor(post, null, null).comments()).singleElement().isEqualTo(written);

        assertThat(publishedEvents.ofType(PostCommented.class)).singleElement().satisfies(event -> {
            assertThat(event.postId()).isEqualTo(post);
            assertThat(event.commentId()).isEqualTo(written.commentId());
            assertThat(event.viewer()).isEqualTo(author);
            assertThat(event.commentedAt()).isEqualTo(now);
        });
    }

    @Test
    void everyNewCommentAnnouncesItsOwnEvent(AssertablePublishedEvents publishedEvents) {
        thread.comment(author, post, "one");
        thread.comment(author, post, "two");

        assertThat(publishedEvents.ofType(PostCommented.class)).hasSize(2);
    }

    @Test
    void theBodyIsTrimmed() {
        PostComment written = thread.comment(author, post, "  spaced out  ");

        assertThat(written.body()).isEqualTo("spaced out");
    }

    @Test
    void aBlankBodyIsRejected() {
        assertThatExceptionOfType(EmptyCommentException.class).isThrownBy(() -> thread.comment(author, post, "   "));
    }

    @Test
    void aBodyOverAThousandCharactersIsRejected() {
        String tooLong = "x".repeat(CommentBody.MAX_LENGTH + 1);

        assertThatExceptionOfType(CommentTooLongException.class)
            .isThrownBy(() -> thread.comment(author, post, tooLong));
    }

    @Test
    void aBodyOfExactlyAThousandCharactersIsAccepted() {
        String atLimit = "x".repeat(CommentBody.MAX_LENGTH);

        assertThat(thread.comment(author, post, atLimit).body()).hasSize(CommentBody.MAX_LENGTH);
    }

    @Test
    void commentingOnYourOwnPostIsAllowed() {
        assertThat(thread.comment(author, new PostId(author.value()), "talking to myself")).isNotNull();
    }

    @Test
    void listsCommentsOldestFirst() {
        UUID first = commentAt("2026-09-04T10:00:00Z", "morning");
        UUID second = commentAt("2026-09-04T11:00:00Z", "noon");

        List<UUID> ids = thread.pageFor(post, null, null).comments().stream().map(PostComment::commentId).toList();

        assertThat(ids).containsExactly(first, second);
    }

    @Test
    void pagesForwardThroughTheThreadWithNoDuplicatesOrSkips() {
        List<UUID> inOrder = new ArrayList<>();
        for (int minute = 0; minute < 5; minute++) {
            inOrder.add(commentAt("2026-09-04T10:0%d:00Z".formatted(minute), "c" + minute));
        }

        assertThat(drain(2)).containsExactlyElementsOf(inOrder);
    }

    @Test
    void breaksACreatedAtTieOnTheCommentIdSoPagingStaysStable() {
        commentAt("2026-09-04T10:00:00Z", "a");
        commentAt("2026-09-04T10:00:00Z", "b");
        commentAt("2026-09-04T10:00:00Z", "c");

        List<UUID> wholePage = thread.pageFor(post, null, 10).comments().stream().map(PostComment::commentId).toList();

        assertThat(drain(1)).hasSize(3).doesNotHaveDuplicates().containsExactlyElementsOf(wholePage);
    }

    @Test
    void clampsTheLimitAndDefaultsWhenAbsent() {
        for (int i = 0; i < CommentThread.MAX_LIMIT + 5; i++) {
            commentAt("2026-09-04T10:00:00Z", "c" + i);
        }

        assertThat(thread.pageFor(post, null, null).comments()).hasSize(CommentThread.DEFAULT_LIMIT);
        assertThat(thread.pageFor(post, null, 1000).comments()).hasSize(CommentThread.MAX_LIMIT);
        assertThat(thread.pageFor(post, null, 0).comments()).hasSize(1);
    }

    @Test
    void aPostWithNoCommentsGetsAnEmptyLastPage() {
        CommentThread.Page page = thread.pageFor(PostId.random(), null, null);

        assertThat(page.comments()).isEmpty();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void theThreadIsScopedToOnePost() {
        commentAt("2026-09-04T10:00:00Z", "on this post");
        PostId other = PostId.random();
        now = Instant.parse("2026-09-04T10:00:00Z");
        thread.comment(ViewerId.random(), other, "on another post");

        assertThat(thread.pageFor(post, null, null).comments()).hasSize(1);
    }

    @Test
    void theCommentsAuthorCanDeleteItAndTheDeletionIsAnnounced(AssertablePublishedEvents publishedEvents) {
        UUID commentId = thread.comment(author, post, "my mistake").commentId();

        thread.delete(author, commentId);

        assertThat(thread.pageFor(post, null, null).comments()).isEmpty();
        assertThat(publishedEvents.ofType(CommentDeleted.class)).singleElement().satisfies(event -> {
            assertThat(event.postId()).isEqualTo(post);
            assertThat(event.commentId()).isEqualTo(commentId);
            assertThat(event.viewer()).isEqualTo(author);
            assertThat(event.deletedAt()).isEqualTo(now);
        });
    }

    @Test
    void thePostsAuthorCanDeleteSomeoneElsesCommentAndTheEventNamesThem(AssertablePublishedEvents publishedEvents) {
        UUID commentId = thread.comment(author, post, "not your call").commentId();

        thread.delete(postAuthor, commentId);

        assertThat(thread.pageFor(post, null, null).comments()).isEmpty();
        assertThat(publishedEvents.ofType(CommentDeleted.class)).singleElement()
            .satisfies(event -> assertThat(event.viewer()).isEqualTo(postAuthor));
    }

    @Test
    void aStrangerCannotDeleteAndTheCommentStands(AssertablePublishedEvents publishedEvents) {
        UUID commentId = thread.comment(author, post, "leave it").commentId();

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> thread.delete(ViewerId.random(), commentId));

        assertThat(thread.pageFor(post, null, null).comments()).hasSize(1);
        assertThat(publishedEvents.ofType(CommentDeleted.class)).isEmpty();
    }

    @Test
    void deletingACommentThatIsNotThereIsASilentNoOp(AssertablePublishedEvents publishedEvents) {
        thread.delete(author, UUID.randomUUID());

        assertThat(publishedEvents.ofType(CommentDeleted.class)).isEmpty();
    }

    // The batch comment-count read (CommentCounts) is pinned by the
    // consumer-contract test
    // CommentCountsContractTest (#157), not here.

    // ---- onPostDeleted() : thread cleanup ----

    @Test
    void deletingAPostHardDeletesItsThreadAndLeavesOtherThreadsAlone() {
        var deleted = PostId.random();
        var untouched = PostId.random();
        thread.comment(ViewerId.random(), deleted, "on the doomed post");
        thread.comment(ViewerId.random(), deleted, "also doomed");
        thread.comment(ViewerId.random(), untouched, "still here");

        events.publishEvent(new PostDeleted(deleted, UserId.random(), MediaId.random(), Instant.now()));

        assertThat(thread.pageFor(deleted, null, null).comments()).isEmpty();
        assertThat(thread.pageFor(untouched, null, null).comments()).hasSize(1);
    }

    private UUID commentAt(String instant, String body) {
        now = Instant.parse(instant);
        return thread.comment(ViewerId.random(), post, body).commentId();
    }

    private List<UUID> drain(int pageSize) {
        List<UUID> ids = new ArrayList<>();
        Cursor cursor = null;
        do {
            CommentThread.Page page = thread.pageFor(post, cursor, pageSize);
            assertThat(page.comments()).hasSizeLessThanOrEqualTo(pageSize);
            page.comments().forEach(comment -> ids.add(comment.commentId()));
            cursor = page.nextCursor();
        } while (cursor != null);
        return ids;
    }
}
