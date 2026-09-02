package me.imshy.pictogram.engagement.internal;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import me.imshy.pictogram.engagement.LikeCounts.PostLikes;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LikeTallyTest extends EngagementModuleIntegrationTest {

    @Autowired
    Liking liking;

    @Autowired
    LikeTally tally;

    @Test
    void reportsEachRequestedPostsCountAndWhetherTheViewerLikedIt() {
        var ada = ViewerId.random();
        var bob = ViewerId.random();
        var liked = PostId.random();
        var likedByOthersOnly = PostId.random();

        liking.like(ada, liked);
        liking.like(bob, liked);
        liking.like(bob, likedByOthersOnly);

        Map<PostId, PostLikes> byId = index(tally.of(ada, List.of(liked, likedByOthersOnly)));

        assertThat(byId.get(liked)).isEqualTo(new PostLikes(liked, 2, true));
        assertThat(byId.get(likedByOthersOnly)).isEqualTo(new PostLikes(likedByOthersOnly, 1, false));
    }

    @Test
    void returnsARecordForEveryRequestedIdIncludingOneWithNoLikes() {
        var untouched = PostId.random();

        assertThat(tally.of(ViewerId.random(), List.of(untouched))).containsExactly(new PostLikes(untouched, 0, false));
    }

    @Test
    void anEmptyRequestReturnsNothing() {
        assertThat(tally.of(ViewerId.random(), List.of())).isEmpty();
    }

    @Test
    void withoutAViewerReportsEachCountAndNeverAViewerLike() {
        var bob = ViewerId.random();
        var liked = PostId.random();
        var untouched = PostId.random();

        liking.like(bob, liked);
        liking.like(ViewerId.random(), liked);

        Map<PostId, PostLikes> byId = index(tally.of(List.of(liked, untouched)));

        assertThat(byId.get(liked)).isEqualTo(new PostLikes(liked, 2, false));
        assertThat(byId.get(untouched)).isEqualTo(new PostLikes(untouched, 0, false));
    }

    @Test
    void anEmptyRequestWithoutAViewerReturnsNothing() {
        assertThat(tally.of(List.of())).isEmpty();
    }

    private static Map<PostId, PostLikes> index(List<PostLikes> rows) {
        return rows.stream().collect(toMap(PostLikes::post, identity()));
    }
}
