package me.imshy.pictogram.social.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.social.SocialStats;
import me.imshy.pictogram.social.internal.SocialModuleIntegrationTest;
import me.imshy.pictogram.social.internal.comment.CommentThread;
import me.imshy.pictogram.social.internal.follow.Following;
import me.imshy.pictogram.social.internal.likes.Liking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// The consumer-contract test for the published SocialStats interface (#233): the
// product-stat gauge job in :app is its one caller. Drives the three sub-domains it
// aggregates the same way their own tests do.
class SocialStatsContractTest extends SocialModuleIntegrationTest {

    @Autowired
    Following following;

    @Autowired
    Liking liking;

    @Autowired
    CommentThread comments;

    @Autowired
    SocialStats socialStats;

    @MockitoBean
    Clock clock;

    private final PostId post = PostId.random();

    @BeforeEach
    void bindClockAndPostAuthor() {
        given(clock.instant()).willReturn(Instant.parse("2026-09-11T12:00:00Z"));
        given(publishedPosts.authorOf(post)).willReturn(Optional.of(UserId.random()));
    }

    @Test
    void totalsEachSubDomainIndependently() {
        following.follow(ViewerId.random(), UserId.random());
        following.follow(ViewerId.random(), UserId.random());
        liking.like(ViewerId.random(), post);
        comments.comment(ViewerId.random(), post, "first!");
        comments.comment(ViewerId.random(), post, "second!");
        comments.comment(ViewerId.random(), post, "third!");

        assertThat(socialStats.totalFollows()).isEqualTo(2);
        assertThat(socialStats.totalLikes()).isEqualTo(1);
        assertThat(socialStats.totalComments()).isEqualTo(3);
    }

    @Test
    void anUnlikeOrUnfollowDropsTheTotal() {
        var viewer = ViewerId.random();
        var followed = UserId.random();
        following.follow(viewer, followed);
        liking.like(viewer, post);

        following.unfollow(viewer, followed);
        liking.unlike(viewer, post);

        assertThat(socialStats.totalFollows()).isZero();
        assertThat(socialStats.totalLikes()).isZero();
    }
}
