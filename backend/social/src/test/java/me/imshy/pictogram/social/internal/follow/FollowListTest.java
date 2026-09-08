package me.imshy.pictogram.social.internal.follow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.shared.http.Cursor;
import me.imshy.pictogram.social.internal.SocialModuleIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class FollowListTest extends SocialModuleIntegrationTest {

    @Autowired
    Following following;

    @Autowired
    FollowList followList;

    @MockitoBean
    Clock clock;

    private Instant now = Instant.parse("2026-09-01T12:00:00Z");

    @BeforeEach
    void bindClockToControlledTime() {
        given(clock.instant()).willAnswer(invocation -> now);
    }

    @Test
    void listsFollowersNewestRelationshipFirst() {
        var carol = UserId.random();
        var ada = followAt("2026-09-01T10:00:00Z", carol);
        var bob = followAt("2026-09-01T11:00:00Z", carol);

        FollowList.Page followers = followList.followersOf(carol, null, null);

        assertThat(followers.items()).containsExactly(bob, ada);
        assertThat(followers.nextCursor()).isNull();
    }

    @Test
    void listsTheUsersAViewerFollowsNewestRelationshipFirst() {
        var ada = ViewerId.random();
        var bob = UserId.random();
        var carol = UserId.random();
        now = Instant.parse("2026-09-01T10:00:00Z");
        following.follow(ada, bob);
        now = Instant.parse("2026-09-01T11:00:00Z");
        following.follow(ada, carol);

        FollowList.Page followed = followList.followingOf(ada.asUserId(), null, null);

        assertThat(followed.items()).containsExactly(carol, bob);
    }

    @Test
    void pagesFollowersAcrossCursorsWithNoDuplicatesOrSkips() {
        var target = UserId.random();
        List<UserId> followedInOrder = new ArrayList<>();
        for (int minute = 0; minute < 5; minute++) {
            followedInOrder.add(followAt("2026-09-01T10:0%d:00Z".formatted(minute), target));
        }

        List<UserId> seen = drainFollowers(target, 2);

        assertThat(seen).containsExactlyElementsOf(followedInOrder.reversed());
    }

    @Test
    void breaksAFollowedAtTieOnTheFollowerSoFollowerPagingStaysStable() {
        var target = UserId.random();
        followAt("2026-09-01T10:00:00Z", target);
        followAt("2026-09-01T10:00:00Z", target);
        followAt("2026-09-01T10:00:00Z", target);

        List<UserId> wholePage = followList.followersOf(target, null, 10).items();
        List<UserId> oneAtATime = drainFollowers(target, 1);

        assertThat(oneAtATime).hasSize(3).doesNotHaveDuplicates();
        assertThat(oneAtATime).containsExactlyElementsOf(wholePage);
    }

    @Test
    void breaksAFollowedAtTieOnTheFollowedUserSoFollowingPagingStaysStable() {
        var viewer = ViewerId.random();
        now = Instant.parse("2026-09-01T10:00:00Z");
        for (int i = 0; i < 3; i++) {
            following.follow(viewer, UserId.random());
        }

        List<UserId> wholePage = followList.followingOf(viewer.asUserId(), null, 10).items();
        List<UserId> oneAtATime = drainFollowing(viewer.asUserId(), 1);

        assertThat(oneAtATime).hasSize(3).doesNotHaveDuplicates();
        assertThat(oneAtATime).containsExactlyElementsOf(wholePage);
    }

    @Test
    void clampsTheLimitAndDefaultsWhenAbsent() {
        var target = UserId.random();
        for (int i = 0; i < FollowList.MAX_LIMIT + 5; i++) {
            followAt("2026-09-01T10:00:00Z", target);
        }

        assertThat(followList.followersOf(target, null, null).items()).hasSize(FollowList.DEFAULT_LIMIT);
        assertThat(followList.followersOf(target, null, 1000).items()).hasSize(FollowList.MAX_LIMIT);
        assertThat(followList.followersOf(target, null, 0).items()).hasSize(1);
    }

    @Test
    void anUnfollowDropsTheRowFromTheList() {
        var carol = UserId.random();
        var ada = followAt("2026-09-01T10:00:00Z", carol);
        var bob = followAt("2026-09-01T11:00:00Z", carol);

        following.unfollow(ViewerId.of(ada), carol);

        assertThat(followList.followersOf(carol, null, null).items()).containsExactly(bob);
    }

    @Test
    void aUserWithNoFollowersGetsAnEmptyLastPage() {
        FollowList.Page page = followList.followersOf(UserId.random(), null, null);

        assertThat(page.items()).isEmpty();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void aFinalPageOfExactlyTheLimitCarriesNoNextCursor() {
        var target = UserId.random();
        for (int minute = 0; minute < 3; minute++) {
            followAt("2026-09-01T10:0%d:00Z".formatted(minute), target);
        }

        FollowList.Page page = followList.followersOf(target, null, 3);

        assertThat(page.items()).hasSize(3);
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void oneRowBeyondTheLimitYieldsAFullPageAndACursor() {
        var target = UserId.random();
        for (int minute = 0; minute < 4; minute++) {
            followAt("2026-09-01T10:0%d:00Z".formatted(minute), target);
        }

        FollowList.Page page = followList.followersOf(target, null, 3);

        assertThat(page.items()).hasSize(3);
        assertThat(page.nextCursor()).isNotNull();
    }

    private UserId followAt(String instant, UserId followed) {
        now = Instant.parse(instant);
        var follower = ViewerId.random();
        following.follow(follower, followed);
        return follower.asUserId();
    }

    private List<UserId> drainFollowers(UserId target, int pageSize) {
        return drain(pageSize, cursor -> followList.followersOf(target, cursor, pageSize));
    }

    private List<UserId> drainFollowing(UserId target, int pageSize) {
        return drain(pageSize, cursor -> followList.followingOf(target, cursor, pageSize));
    }

    private List<UserId> drain(int pageSize, Function<Cursor, FollowList.Page> nextPage) {
        List<UserId> ids = new ArrayList<>();
        Cursor cursor = null;
        do {
            FollowList.Page page = nextPage.apply(cursor);
            assertThat(page.items()).hasSizeLessThanOrEqualTo(pageSize);
            ids.addAll(page.items());
            cursor = page.nextCursor();
        } while (cursor != null);
        return ids;
    }
}
