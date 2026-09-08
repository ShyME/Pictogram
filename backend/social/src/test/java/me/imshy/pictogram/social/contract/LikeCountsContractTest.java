package me.imshy.pictogram.social.contract;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.social.LikeCounts;
import me.imshy.pictogram.social.LikeCounts.PostLikes;
import me.imshy.pictogram.social.internal.SocialModuleIntegrationTest;
import me.imshy.pictogram.social.internal.likes.Liking;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

// The consumer-contract test for the published LikeCounts interface (#157): drives it
// through the published type as the likes web layer does. Promoted here from the former
// LikeTallyTest so a breaking shape change fails at the module boundary.
class LikeCountsContractTest extends SocialModuleIntegrationTest {

    @Autowired
    Liking liking;

    @Autowired
    LikeCounts likeCounts;

    @Test
    void reportsEachRequestedPostsCountAndWhetherTheViewerLikedIt() {
        var ada = ViewerId.random();
        var bob = ViewerId.random();
        var liked = PostId.random();
        var likedByOthersOnly = PostId.random();

        liking.like(ada, liked);
        liking.like(bob, liked);
        liking.like(bob, likedByOthersOnly);

        var rows = likeCounts.of(ada, List.of(liked, likedByOthersOnly));
        Map<PostId, PostLikes> byId = index(rows);

        assertThat(rows).extracting(PostLikes::post).containsExactlyInAnyOrder(liked, likedByOthersOnly);
        assertThat(byId.get(liked)).isEqualTo(new PostLikes(liked, 2, true));
        assertThat(byId.get(likedByOthersOnly)).isEqualTo(new PostLikes(likedByOthersOnly, 1, false));
    }

    @Test
    void returnsARecordForEveryRequestedIdIncludingOneWithNoLikes() {
        var untouched = PostId.random();

        assertThat(likeCounts.of(ViewerId.random(), List.of(untouched)))
            .containsExactly(new PostLikes(untouched, 0, false));
    }

    @Test
    void anEmptyRequestReturnsNothing() {
        assertThat(likeCounts.of(ViewerId.random(), List.of())).isEmpty();
    }

    @Test
    void withoutAViewerReportsEachCountAndNeverAViewerLike() {
        var bob = ViewerId.random();
        var liked = PostId.random();
        var untouched = PostId.random();

        liking.like(bob, liked);
        liking.like(ViewerId.random(), liked);

        Map<PostId, PostLikes> byId = index(likeCounts.of(List.of(liked, untouched)));

        assertThat(byId.get(liked)).isEqualTo(new PostLikes(liked, 2, false));
        assertThat(byId.get(untouched)).isEqualTo(new PostLikes(untouched, 0, false));
    }

    @Test
    void anEmptyRequestWithoutAViewerReturnsNothing() {
        assertThat(likeCounts.of(List.of())).isEmpty();
    }

    private static Map<PostId, PostLikes> index(List<PostLikes> rows) {
        return rows.stream().collect(toMap(PostLikes::post, identity()));
    }
}
