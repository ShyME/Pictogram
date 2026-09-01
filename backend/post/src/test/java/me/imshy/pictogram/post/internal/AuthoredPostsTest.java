package me.imshy.pictogram.post.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.imshy.pictogram.post.PublishedPost;
import me.imshy.pictogram.post.PublishedPosts;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.http.Cursor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class AuthoredPostsTest extends PostModuleIntegrationTest {

    @Autowired
    Publishing publishing;

    @Autowired
    PostDeletion deletion;

    @Autowired
    PublishedPosts publishedPosts;

    @MockitoBean
    Clock clock;

    private Instant now = Instant.parse("2026-09-01T12:00:00Z");

    @BeforeEach
    void bindClockToControlledTime() {
        given(clock.instant()).willAnswer(invocation -> now);
    }

    @Test
    void mergesSeveralAuthorsPostsIntoOneNewestFirstStream() {
        var ada = UserId.random();
        var bob = UserId.random();

        var a1 = publishAt(ada, "2026-09-01T10:00:00Z");
        var b1 = publishAt(bob, "2026-09-01T10:30:00Z");
        var a2 = publishAt(ada, "2026-09-01T11:00:00Z");
        var b2 = publishAt(bob, "2026-09-01T11:30:00Z");

        assertThat(ids(publishedPosts.byAuthors(List.of(ada, bob), null, 10))).containsExactly(b2, a2, b1, a1);
    }

    @Test
    void pagesAcrossAuthorsWithoutDuplicatesOrSkips() {
        var ada = UserId.random();
        var bob = UserId.random();
        List<PostId> published = new ArrayList<>();
        for (int minute = 0; minute < 6; minute++) {
            var author = minute % 2 == 0 ? ada : bob;
            published.add(publishAt(author, "2026-09-01T10:0%d:00Z".formatted(minute)));
        }

        List<PostId> seen = drain(List.of(ada, bob), 2);

        assertThat(seen)
                .containsExactly(
                        published.get(5),
                        published.get(4),
                        published.get(3),
                        published.get(2),
                        published.get(1),
                        published.get(0));
    }

    @Test
    void breaksAPublishedAtTieOnTheIdSoPagingStaysStable() {
        var ada = UserId.random();
        var bob = UserId.random();
        publishAt(ada, "2026-09-01T10:00:00Z");
        publishAt(bob, "2026-09-01T10:00:00Z");
        publishAt(ada, "2026-09-01T10:00:00Z");
        publishAt(bob, "2026-09-01T10:00:00Z");

        List<PostId> wholePage = ids(publishedPosts.byAuthors(List.of(ada, bob), null, 10));
        List<PostId> pagedOneAtATime = drain(List.of(ada, bob), 1);

        assertThat(pagedOneAtATime).hasSize(4).doesNotHaveDuplicates();
        assertThat(pagedOneAtATime).containsExactlyElementsOf(wholePage);
    }

    @Test
    void onlyReturnsPostsByTheRequestedAuthors() {
        var ada = UserId.random();
        var bob = UserId.random();
        var carol = UserId.random();
        var adasPost = publishAt(ada, "2026-09-01T10:00:00Z");
        publishAt(bob, "2026-09-01T11:00:00Z");
        var carolsPost = publishAt(carol, "2026-09-01T12:00:00Z");

        assertThat(ids(publishedPosts.byAuthors(List.of(ada, carol), null, 10))).containsExactly(carolsPost, adasPost);
    }

    @Test
    void aDeletedPostLeavesTheStream() {
        var ada = UserId.random();
        var keep = publishAt(ada, "2026-09-01T10:00:00Z");
        var drop = publishAt(ada, "2026-09-01T11:00:00Z");

        deletion.delete(ada, drop);

        assertThat(ids(publishedPosts.byAuthors(List.of(ada), null, 10))).containsExactly(keep);
    }

    @Test
    void noRequestedAuthorsIsAnEmptyLastPage() {
        PublishedPosts.Page page = publishedPosts.byAuthors(List.of(), null, 10);

        assertThat(page.posts()).isEmpty();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void theLastPageHasNoNextCursor() {
        var ada = UserId.random();
        publishAt(ada, "2026-09-01T10:00:00Z");
        publishAt(ada, "2026-09-01T11:00:00Z");

        assertThat(publishedPosts.byAuthors(List.of(ada), null, 5).nextCursor()).isNull();
    }

    private PostId publishAt(UserId author, String instant) {
        now = Instant.parse(instant);
        var mediaId = MediaId.random();
        given(media.ownerOf(mediaId)).willReturn(Optional.of(author));
        return publishing.publish(author, mediaId, null).postId();
    }

    private static List<PostId> ids(PublishedPosts.Page page) {
        return page.posts().stream().map(PublishedPost::postId).toList();
    }

    private List<PostId> drain(List<UserId> authors, int pageSize) {
        List<PostId> ids = new ArrayList<>();
        Cursor cursor = null;
        do {
            PublishedPosts.Page page = publishedPosts.byAuthors(authors, cursor, pageSize);
            ids.addAll(ids(page));
            cursor = page.nextCursor();
        } while (cursor != null);
        return ids;
    }
}
