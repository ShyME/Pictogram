package me.imshy.pictogram.follow.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import me.imshy.pictogram.follow.UserFollowed;
import me.imshy.pictogram.follow.UserUnfollowed;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.ViewerId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.AssertablePublishedEvents;

class FollowingTest extends FollowModuleIntegrationTest {

    @Autowired
    Following following;

    @Autowired
    FollowDirectory graph;

    private final ViewerId ada = ViewerId.random();
    private final UserId bob = UserId.random();

    @Test
    void followingAUserAddsTheEdgeAndAnnouncesIt(AssertablePublishedEvents events) {
        following.follow(ada, bob);

        assertThat(graph.isFollowing(ada, bob)).isTrue();
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

        assertThat(graph.followerCount(bob)).isEqualTo(1);
        assertThat(events.ofType(UserFollowed.class)).hasSize(1);
    }

    @Test
    void unfollowingRemovesTheEdgeAndAnnouncesIt(AssertablePublishedEvents events) {
        following.follow(ada, bob);

        following.unfollow(ada, bob);

        assertThat(graph.isFollowing(ada, bob)).isFalse();
        assertThat(events.ofType(UserUnfollowed.class)).singleElement().satisfies(event -> {
            assertThat(event.follower()).isEqualTo(ada.asUserId());
            assertThat(event.followed()).isEqualTo(bob);
        });
    }

    @Test
    void unfollowingAUserNotFollowedChangesNothingAndAnnouncesNothing(AssertablePublishedEvents events) {
        following.unfollow(ada, bob);

        assertThat(graph.isFollowing(ada, bob)).isFalse();
        assertThat(events.ofType(UserUnfollowed.class)).isEmpty();
    }

    @Test
    void aUserCannotFollowThemselves(AssertablePublishedEvents events) {
        var self = ViewerId.random();

        assertThatExceptionOfType(SelfFollowException.class).isThrownBy(() -> following.follow(self, self.asUserId()));

        assertThat(graph.followingCount(self.asUserId())).isZero();
        assertThat(events.ofType(UserFollowed.class)).isEmpty();
    }
}
