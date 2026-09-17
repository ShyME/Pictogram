package me.imshy.pictogram.social.internal.feed;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import me.imshy.pictogram.post.PublishedPosts;
import org.junit.jupiter.api.Test;

class GlobalExploreFeedTest {

    private final PublishedPosts posts = mock(PublishedPosts.class);
    private final GlobalExploreFeed explore = new GlobalExploreFeed(posts);

    @Test
    void theRequestedPageSizeIsClampedBetweenOneAndTheMax() {
        given(posts.page(any(), anyInt())).willReturn(new PublishedPosts.Page(List.of(), null));

        explore.pageFor(null, null);
        verify(posts).page(any(), eq(FeedPageSize.DEFAULT));

        explore.pageFor(null, 1000);
        verify(posts).page(any(), eq(FeedPageSize.MAX));

        explore.pageFor(null, 0);
        verify(posts).page(any(), eq(1));
    }
}
