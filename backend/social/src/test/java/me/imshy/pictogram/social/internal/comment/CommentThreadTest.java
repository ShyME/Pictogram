package me.imshy.pictogram.social.internal.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.shared.http.Cursor;
import me.imshy.pictogram.social.internal.SocialModuleIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class CommentThreadTest extends SocialModuleIntegrationTest {

    @Autowired
    Commenting commenting;

    @Autowired
    CommentThread thread;

    @MockitoBean
    Clock clock;

    private Instant now = Instant.parse("2026-09-04T12:00:00Z");
    private final PostId post = PostId.random();

    @BeforeEach
    void bindClock() {
        given(clock.instant()).willAnswer(invocation -> now);
    }

    @Test
    void listsCommentsOldestFirst() {
        UUID first = commentAt("2026-09-04T10:00:00Z", "morning");
        UUID second = commentAt("2026-09-04T11:00:00Z", "noon");

        List<UUID> ids = thread.pageFor(post, null, null).comments().stream()
                .map(PostComment::commentId)
                .toList();

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

        List<UUID> wholePage = thread.pageFor(post, null, 10).comments().stream()
                .map(PostComment::commentId)
                .toList();

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
        commenting.comment(ViewerId.random(), other, "on another post");

        assertThat(thread.pageFor(post, null, null).comments()).hasSize(1);
    }

    private UUID commentAt(String instant, String body) {
        now = Instant.parse(instant);
        return commenting.comment(ViewerId.random(), post, body).commentId();
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
