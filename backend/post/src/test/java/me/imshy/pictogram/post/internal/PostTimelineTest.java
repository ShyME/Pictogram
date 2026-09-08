package me.imshy.pictogram.post.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.imshy.pictogram.post.internal.PostTimeline.Page;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.http.Cursor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class PostTimelineTest extends PostModuleIntegrationTest {

    @Autowired
    PostPublishing postPublishing;

    @Autowired
    PostTimeline postTimeline;

    @MockitoBean
    Clock clock;

    private Instant now = Instant.parse("2026-09-01T12:00:00Z");

    @BeforeEach
    void bindClockToControlledTime() {
        given(clock.instant()).willAnswer(invocation -> now);
    }

    @Test
    void returnsAnAuthorsPostsNewestFirst() {
        var author = UserId.random();
        var a = publishAt(author, "2026-09-01T10:00:00Z");
        var b = publishAt(author, "2026-09-01T11:00:00Z");
        var c = publishAt(author, "2026-09-01T12:00:00Z");

        assertThat(ids(postTimeline.pageFor(author, null, null))).containsExactly(c, b, a);
    }

    @Test
    void pagesWithoutDuplicatesOrSkips() {
        var author = UserId.random();
        List<PostId> published = new ArrayList<>();
        for (int minute = 0; minute < 5; minute++) {
            published.add(publishAt(author, "2026-09-01T10:0%d:00Z".formatted(minute)));
        }

        List<PostId> seen = drain(author, 2);

        assertThat(seen).containsExactly(published.get(4), published.get(3), published.get(2), published.get(1),
            published.get(0));
    }

    @Test
    void breaksATieOnPublishedAtWithTheIdSoPagingStaysStable() {
        var author = UserId.random();
        publishAt(author, "2026-09-01T10:00:00Z");
        publishAt(author, "2026-09-01T10:00:00Z");
        publishAt(author, "2026-09-01T10:00:00Z");

        List<PostId> wholePage = ids(postTimeline.pageFor(author, null, 10));
        List<PostId> pagedOneAtATime = drain(author, 1);

        assertThat(pagedOneAtATime).hasSize(3).doesNotHaveDuplicates();
        assertThat(pagedOneAtATime).containsExactlyElementsOf(wholePage);
    }

    @Test
    void clampsTheLimitAndDefaultsWhenAbsent() {
        var author = UserId.random();
        for (int i = 0; i < PostTimeline.MAX_LIMIT + 5; i++) {
            publishAt(author, "2026-09-01T10:00:00Z");
        }

        assertThat(postTimeline.pageFor(author, null, null).items()).hasSize(PostTimeline.DEFAULT_LIMIT);
        assertThat(postTimeline.pageFor(author, null, 1000).items()).hasSize(PostTimeline.MAX_LIMIT);
        assertThat(postTimeline.pageFor(author, null, 0).items()).hasSize(1);
    }

    @Test
    void isScopedToTheOneAuthor() {
        var ada = UserId.random();
        var grace = UserId.random();
        var adasPost = publishAt(ada, "2026-09-01T10:00:00Z");
        publishAt(grace, "2026-09-01T11:00:00Z");

        assertThat(ids(postTimeline.pageFor(ada, null, null))).containsExactly(adasPost);
    }

    @Test
    void theLastPageHasNoNextCursor() {
        var author = UserId.random();
        publishAt(author, "2026-09-01T10:00:00Z");
        publishAt(author, "2026-09-01T11:00:00Z");

        assertThat(postTimeline.pageFor(author, null, 5).nextCursor()).isNull();
    }

    @Test
    void aFinalPageOfExactlyTheLimitCarriesNoNextCursor() {
        var author = UserId.random();
        for (int minute = 0; minute < 3; minute++) {
            publishAt(author, "2026-09-01T10:0%d:00Z".formatted(minute));
        }

        Page page = postTimeline.pageFor(author, null, 3);

        assertThat(page.items()).hasSize(3);
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void oneRowBeyondTheLimitYieldsAFullPageAndACursor() {
        var author = UserId.random();
        for (int minute = 0; minute < 4; minute++) {
            publishAt(author, "2026-09-01T10:0%d:00Z".formatted(minute));
        }

        Page page = postTimeline.pageFor(author, null, 3);

        assertThat(page.items()).hasSize(3);
        assertThat(page.nextCursor()).isNotNull();
    }

    private PostId publishAt(UserId author, String instant) {
        now = Instant.parse(instant);
        var mediaId = MediaId.random();
        given(mediaCatalog.ownerOf(mediaId)).willReturn(Optional.of(author));
        return postPublishing.publish(author, mediaId, null).postId();
    }

    private static List<PostId> ids(Page page) {
        return page.items().stream().map(PostView::postId).toList();
    }

    private List<PostId> drain(UserId author, int pageSize) {
        List<PostId> ids = new ArrayList<>();
        Cursor cursor = null;
        do {
            Page page = postTimeline.pageFor(author, cursor, pageSize);
            ids.addAll(ids(page));
            cursor = page.nextCursor();
        } while (cursor != null);
        return ids;
    }
}
