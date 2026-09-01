package me.imshy.pictogram.feed.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import me.imshy.pictogram.follow.FollowGraph;
import me.imshy.pictogram.post.PublishedPosts;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.ViewerId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FanOutOnReadFeedTest {

    private final FollowGraph followGraph = mock(FollowGraph.class);
    private final PublishedPosts posts = mock(PublishedPosts.class);
    private final FanOutOnReadFeed feed = new FanOutOnReadFeed(followGraph, posts);

    @Test
    void aViewerWhoFollowsNobodyGetsAnEmptyPageAndPostsIsNeverAsked() {
        var viewer = ViewerId.random();
        given(followGraph.usersFollowedBy(viewer)).willReturn(List.of());

        FeedQuery.Page page = feed.pageFor(viewer, null, null);

        assertThat(page.posts()).isEmpty();
        assertThat(page.nextCursor()).isNull();
        verify(posts, never()).byAuthors(any(), any(), anyInt());
    }

    @Test
    void theFollowedAuthorsAreHandedStraightToPosts() {
        var viewer = ViewerId.random();
        var followed = List.of(UserId.random(), UserId.random());
        given(followGraph.usersFollowedBy(viewer)).willReturn(followed);
        given(posts.byAuthors(any(), any(), anyInt())).willReturn(new PublishedPosts.Page(List.of(), null));

        feed.pageFor(viewer, null, null);

        ArgumentCaptor<List<UserId>> authors = ArgumentCaptor.captor();
        verify(posts).byAuthors(authors.capture(), any(), anyInt());
        assertThat(authors.getValue()).isEqualTo(followed);
    }

    @Test
    void theRequestedPageSizeIsClampedBetweenOneAndTheMax() {
        var viewer = ViewerId.random();
        given(followGraph.usersFollowedBy(viewer)).willReturn(List.of(UserId.random()));
        given(posts.byAuthors(any(), any(), anyInt())).willReturn(new PublishedPosts.Page(List.of(), null));

        feed.pageFor(viewer, null, null);
        verify(posts).byAuthors(any(), any(), eq(FanOutOnReadFeed.DEFAULT_LIMIT));

        feed.pageFor(viewer, null, 1000);
        verify(posts).byAuthors(any(), any(), eq(FanOutOnReadFeed.MAX_LIMIT));

        feed.pageFor(viewer, null, 0);
        verify(posts).byAuthors(any(), any(), eq(1));
    }
}
