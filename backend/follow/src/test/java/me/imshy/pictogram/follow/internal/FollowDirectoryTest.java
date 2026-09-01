package me.imshy.pictogram.follow.internal;

import static org.assertj.core.api.Assertions.assertThat;

import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.ViewerId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The published {@link me.imshy.pictogram.follow.FollowGraph} read model: the follower and
 * following counts a profile page shows, the "am I following them" the follow button
 * reflects, and the list of followed users {@code feed} will fan out over. Each is derived
 * live from the edge table.
 */
class FollowDirectoryTest extends FollowModuleIntegrationTest {

    @Autowired
    Following following;

    @Autowired
    FollowDirectory graph;

    @Test
    void countsFollowersAndFollowingIndependentlyPerDirection() {
        var ada = ViewerId.random();
        var bob = ViewerId.random();
        var carol = ViewerId.random();

        following.follow(ada, bob.asUserId());
        following.follow(carol, bob.asUserId());
        following.follow(bob, ada.asUserId());

        assertThat(graph.followerCount(bob.asUserId())).isEqualTo(2);
        assertThat(graph.followingCount(bob.asUserId())).isEqualTo(1);
        assertThat(graph.followerCount(ada.asUserId())).isEqualTo(1);
        assertThat(graph.followingCount(carol.asUserId())).isEqualTo(1);
    }

    @Test
    void isFollowingIsDirected() {
        var ada = ViewerId.random();
        var bob = UserId.random();

        following.follow(ada, bob);

        assertThat(graph.isFollowing(ada, bob)).isTrue();
        assertThat(graph.isFollowing(ViewerId.of(bob), ada.asUserId())).isFalse();
    }

    @Test
    void listsExactlyTheUsersAViewerFollows() {
        var ada = ViewerId.random();
        var bob = UserId.random();
        var carol = UserId.random();
        var dave = UserId.random();

        following.follow(ada, bob);
        following.follow(ada, carol);
        following.follow(ViewerId.random(), dave);

        assertThat(graph.usersFollowedBy(ada)).containsExactlyInAnyOrder(bob, carol);
    }
}
