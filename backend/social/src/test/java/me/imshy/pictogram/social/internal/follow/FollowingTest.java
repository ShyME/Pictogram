package me.imshy.pictogram.social.internal.follow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.social.UserFollowed;
import me.imshy.pictogram.social.UserUnfollowed;
import me.imshy.pictogram.social.internal.SocialModuleIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.AssertablePublishedEvents;

class FollowingTest extends SocialModuleIntegrationTest {

    @Autowired
    Following following;

    @Autowired
    FollowDirectory followGraph;

    private final ViewerId ada = ViewerId.random();
    private final UserId bob = UserId.random();

    @Test
    void followingAUserAddsTheEdgeAndAnnouncesIt(AssertablePublishedEvents events) {
        following.follow(ada, bob);

        assertThat(followGraph.isFollowing(ada, bob)).isTrue();
        assertThat(events.ofType(UserFollowed.class)).singleElement().satisfies(event -> {
            assertThat(event.follower()).isEqualTo(ada.asUserId());
            assertThat(event.followed()).isEqualTo(bob);
            assertThat(event.followedAt()).isNotNull();
        });
    }

    @Test
    void followingAUserAlreadyFollowedChangesNothingAndAnnouncesNothing(AssertablePublishedEvents events) {
        following.follow(ada, bob);

        following.follow(ada, bob);

        assertThat(followGraph.followerCount(bob)).isEqualTo(1);
        assertThat(events.ofType(UserFollowed.class)).hasSize(1);
    }

    @Test
    void unfollowingRemovesTheEdgeAndAnnouncesIt(AssertablePublishedEvents events) {
        following.follow(ada, bob);

        following.unfollow(ada, bob);

        assertThat(followGraph.isFollowing(ada, bob)).isFalse();
        assertThat(events.ofType(UserUnfollowed.class)).singleElement().satisfies(event -> {
            assertThat(event.follower()).isEqualTo(ada.asUserId());
            assertThat(event.followed()).isEqualTo(bob);
        });
    }

    @Test
    void unfollowingAUserNotFollowedChangesNothingAndAnnouncesNothing(AssertablePublishedEvents events) {
        following.unfollow(ada, bob);

        assertThat(followGraph.isFollowing(ada, bob)).isFalse();
        assertThat(events.ofType(UserUnfollowed.class)).isEmpty();
    }

    @Test
    void aUserCannotFollowThemselves(AssertablePublishedEvents events) {
        var self = ViewerId.random();

        assertThatExceptionOfType(SelfFollowException.class).isThrownBy(() -> following.follow(self, self.asUserId()));

        assertThat(followGraph.followingCount(self.asUserId())).isZero();
        assertThat(events.ofType(UserFollowed.class)).isEmpty();
    }
}
