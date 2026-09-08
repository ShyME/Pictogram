package me.imshy.pictogram.social.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.social.LikeCounts;
import me.imshy.pictogram.social.LikeCounts.PostLikes;
import me.imshy.pictogram.social.internal.SocialModuleIntegrationTest;
import me.imshy.pictogram.social.internal.likes.Liking;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LikeCountsContractTest extends SocialModuleIntegrationTest {

    @Autowired
    Liking liking;

    @Autowired
    LikeCounts likeCounts;

    @Test
    void theViewerFormReturnsExactlyOneRowPerRequestedPost() {
        var viewer = ViewerId.random();
        var liked = PostId.random();
        var likedByOthers = PostId.random();
        var untouched = PostId.random();
        liking.like(viewer, liked);
        liking.like(ViewerId.random(), likedByOthers);

        var rows = likeCounts.of(viewer, List.of(liked, likedByOthers, untouched));

        assertThat(rows).extracting(PostLikes::post).containsExactlyInAnyOrder(liked, likedByOthers, untouched);
    }

    @Test
    void theNoViewerFormReturnsExactlyOneRowPerRequestedPost() {
        var present = PostId.random();
        var untouched = PostId.random();
        liking.like(ViewerId.random(), present);

        assertThat(likeCounts.of(List.of(present, untouched))).extracting(PostLikes::post)
            .containsExactlyInAnyOrder(present, untouched);
    }

    @Test
    void anUnknownPostReadsAsTheZeroValue() {
        var unknown = PostId.random();

        assertThat(likeCounts.of(ViewerId.random(), List.of(unknown)))
            .containsExactly(new PostLikes(unknown, 0, false));
        assertThat(likeCounts.of(List.of(unknown))).containsExactly(new PostLikes(unknown, 0, false));
    }

    @Test
    void theNoViewerFormNeverLeaksAViewersOwnLikeState() {
        var viewer = ViewerId.random();
        var liked = PostId.random();
        var alsoLiked = PostId.random();
        liking.like(viewer, liked);
        liking.like(viewer, alsoLiked);
        liking.like(ViewerId.random(), liked);

        Map<PostId, PostLikes> withViewer = byId(likeCounts.of(viewer, List.of(liked, alsoLiked)));
        Map<PostId, PostLikes> withoutViewer = byId(likeCounts.of(List.of(liked, alsoLiked)));

        assertThat(withViewer.get(liked)).isEqualTo(new PostLikes(liked, 2, true));
        assertThat(withViewer.get(alsoLiked)).isEqualTo(new PostLikes(alsoLiked, 1, true));
        assertThat(withoutViewer.get(liked)).isEqualTo(new PostLikes(liked, 2, false));
        assertThat(withoutViewer.get(alsoLiked)).isEqualTo(new PostLikes(alsoLiked, 1, false));
    }

    @Test
    void anEmptyRequestReturnsNoRowsInEitherForm() {
        assertThat(likeCounts.of(ViewerId.random(), List.of())).isEmpty();
        assertThat(likeCounts.of(List.of())).isEmpty();
    }

    private static Map<PostId, PostLikes> byId(List<PostLikes> rows) {
        return rows.stream().collect(Collectors.toMap(PostLikes::post, Function.identity()));
    }
}
