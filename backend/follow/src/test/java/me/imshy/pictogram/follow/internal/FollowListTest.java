package me.imshy.pictogram.follow.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.shared.http.Cursor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * The paged reads behind the follower / following list screens (#57): who follows a user,
 * and who a user follows — newest-relationship-first, keyset-paged so an edge added or
 * removed between page fetches causes neither duplicates nor skips, including when several
 * edges share a {@code followedAt} and only the id breaks the tie. These live in
 * {@code follow.internal}: the web layer needs them, {@code feed} does not, so
 * {@link me.imshy.pictogram.follow.FollowGraph} stays as narrow as #17 left it.
 */
class FollowListTest extends FollowModuleIntegrationTest {

    @Autowired
    Following following;

    @Autowired
    FollowList lists;

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

        FollowList.Page followers = lists.followersOf(carol, null, null);

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

        FollowList.Page followed = lists.followingOf(ada.asUserId(), null, null);

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
    void breaksATieOnFollowedAtWithTheIdSoPagingStaysStable() {
        var target = UserId.random();
        followAt("2026-09-01T10:00:00Z", target);
        followAt("2026-09-01T10:00:00Z", target);
        followAt("2026-09-01T10:00:00Z", target);

        List<UserId> wholePage = lists.followersOf(target, null, 10).items();
        List<UserId> oneAtATime = drainFollowers(target, 1);

        assertThat(oneAtATime).hasSize(3).doesNotHaveDuplicates();
        assertThat(oneAtATime).containsExactlyElementsOf(wholePage);
    }

    @Test
    void clampsTheLimitAndDefaultsWhenAbsent() {
        var target = UserId.random();
        for (int i = 0; i < FollowList.MAX_LIMIT + 5; i++) {
            followAt("2026-09-01T10:00:00Z", target);
        }

        assertThat(lists.followersOf(target, null, null).items()).hasSize(FollowList.DEFAULT_LIMIT);
        assertThat(lists.followersOf(target, null, 1000).items()).hasSize(FollowList.MAX_LIMIT);
        assertThat(lists.followersOf(target, null, 0).items()).hasSize(1);
    }

    @Test
    void anUnfollowDropsTheRowFromTheList() {
        var carol = UserId.random();
        var ada = followAt("2026-09-01T10:00:00Z", carol);
        var bob = followAt("2026-09-01T11:00:00Z", carol);

        following.unfollow(ViewerId.of(ada), carol);

        assertThat(lists.followersOf(carol, null, null).items()).containsExactly(bob);
    }

    @Test
    void aUserWithNoFollowersGetsAnEmptyLastPage() {
        FollowList.Page page = lists.followersOf(UserId.random(), null, null);

        assertThat(page.items()).isEmpty();
        assertThat(page.nextCursor()).isNull();
    }

    private UserId followAt(String instant, UserId followed) {
        now = Instant.parse(instant);
        var follower = ViewerId.random();
        following.follow(follower, followed);
        return follower.asUserId();
    }

    private List<UserId> drainFollowers(UserId target, int pageSize) {
        List<UserId> ids = new ArrayList<>();
        Cursor cursor = null;
        do {
            FollowList.Page page = lists.followersOf(target, cursor, pageSize);
            assertThat(page.items()).hasSizeLessThanOrEqualTo(pageSize);
            ids.addAll(page.items());
            cursor = page.nextCursor();
        } while (cursor != null);
        return ids;
    }
}
