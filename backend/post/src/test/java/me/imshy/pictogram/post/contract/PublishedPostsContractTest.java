package me.imshy.pictogram.post.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.imshy.pictogram.post.PublishedPost;
import me.imshy.pictogram.post.PublishedPosts;
import me.imshy.pictogram.post.internal.PostModuleIntegrationTest;
import me.imshy.pictogram.post.internal.PostPublishing;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.http.Cursor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class PublishedPostsContractTest extends PostModuleIntegrationTest {

    @Autowired
    PostPublishing postPublishing;

    @Autowired
    PublishedPosts publishedPosts;

    @MockitoBean
    Clock clock;

    private Instant now = Instant.parse("2026-09-01T12:00:00Z");

    @BeforeEach
    void bindClock() {
        given(clock.instant()).willAnswer(invocation -> now);
    }

    @Test
    void byAuthorsReturnsPostsOnlyForTheRequestedAuthorsNewestFirst() {
        var ada = UserId.random();
        var bob = UserId.random();
        var carol = UserId.random();
        var adaOld = publishAt(ada, "2026-09-01T10:00:00Z");
        publishAt(bob, "2026-09-01T10:30:00Z");
        var carolNew = publishAt(carol, "2026-09-01T11:00:00Z");

        var page = publishedPosts.byAuthors(List.of(ada, carol), null, 10);

        assertThat(ids(page)).containsExactly(carolNew, adaOld);
    }

    @Test
    void anAuthorWithNoPostsContributesNothingRatherThanFailing() {
        var ada = UserId.random();
        var silent = UserId.random();
        var only = publishAt(ada, "2026-09-01T10:00:00Z");

        var page = publishedPosts.byAuthors(List.of(ada, silent), null, 10);

        assertThat(ids(page)).containsExactly(only);
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void noRequestedAuthorsIsAnEmptyTerminalPage() {
        var page = publishedPosts.byAuthors(List.of(), null, 10);

        assertThat(page.posts()).isEmpty();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void theKeysetPageWalksEveryPostOfTheRequestedAuthorsExactlyOnce() {
        var ada = UserId.random();
        var bob = UserId.random();
        List<PostId> published = new ArrayList<>();
        for (int minute = 0; minute < 6; minute++) {
            published.add(publishAt(minute % 2 == 0 ? ada : bob, "2026-09-01T10:0%d:00Z".formatted(minute)));
        }

        List<PostId> wholePage = ids(publishedPosts.byAuthors(List.of(ada, bob), null, 10));
        List<PostId> walked = walk(List.of(ada, bob), 2);

        assertThat(walked).doesNotHaveDuplicates().containsExactlyElementsOf(wholePage);
        assertThat(walked).containsExactlyInAnyOrderElementsOf(published);
    }

    @Test
    void authorOfNamesAKnownPostAndIsEmptyForAnUnknownId() {
        var ada = UserId.random();
        var post = publishAt(ada, "2026-09-01T10:00:00Z");

        assertThat(publishedPosts.authorOf(post)).contains(ada);
        assertThat(publishedPosts.authorOf(PostId.random())).isEmpty();
    }

    private PostId publishAt(UserId author, String instant) {
        now = Instant.parse(instant);
        var mediaId = MediaId.random();
        given(mediaCatalog.ownerOf(mediaId)).willReturn(Optional.of(author));
        return postPublishing.publish(author, mediaId, null).postId();
    }

    private static List<PostId> ids(PublishedPosts.Page page) {
        return page.posts().stream().map(PublishedPost::postId).toList();
    }

    private List<PostId> walk(List<UserId> authors, int pageSize) {
        List<PostId> seen = new ArrayList<>();
        Cursor cursor = null;
        do {
            PublishedPosts.Page page = publishedPosts.byAuthors(authors, cursor, pageSize);
            seen.addAll(ids(page));
            cursor = page.nextCursor();
        } while (cursor != null);
        return seen;
    }
}
