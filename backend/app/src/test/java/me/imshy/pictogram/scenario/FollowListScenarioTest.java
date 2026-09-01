package me.imshy.pictogram.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import me.imshy.pictogram.scenario.PictogramApi.AccountPage;
import me.imshy.pictogram.scenario.PictogramApi.Actor;
import me.imshy.pictogram.scenario.PictogramApi.FollowRelationship;
import org.junit.jupiter.api.Test;

/**
 * The ticket's broad scenario (#57): from a few follows between three people, each person's
 * follower and following list reads back newest-relationship-first; a small page size splits
 * a list across cursors with no duplicates or skips; and an unfollow drops the row.
 */
class FollowListScenarioTest extends ScenarioTest {

    @Test
    void followerAndFollowingListsReadBackNewestFirstPageCleanlyAndTrackUnfollows() {
        var ada = pictogram.registerViaGoogle("ada@example.com");
        String adaId = ada.completeOnboarding("ada_lists", "Ada", null).userId();
        var bob = pictogram.registerViaGoogle("bob@example.com");
        String bobId = bob.completeOnboarding("bob_lists", "Bob", null).userId();
        var carol = pictogram.registerViaGoogle("carol@example.com");
        String carolId = carol.completeOnboarding("carol_lists", "Carol", null).userId();

        // Ada follows Bob, then Carol; Carol then follows Bob.
        ada.follow(bobId);
        ada.follow(carolId);
        carol.follow(bobId);

        // Bob's followers, newest follow first: Carol (just now), then Ada.
        assertThat(drainFollowers(ada, bobId, null)).containsExactly(carolId, adaId);
        // Ada's following, newest first: Carol, then Bob.
        assertThat(drainFollowing(ada, adaId, null)).containsExactly(carolId, bobId);
        // Carol only follows Bob.
        assertThat(drainFollowing(carol, carolId, null)).containsExactly(bobId);

        // A page size of 1 splits Bob's two followers across cursors without dupes or skips.
        assertThat(drainFollowers(ada, bobId, 1)).containsExactly(carolId, adaId);
        AccountPage firstPage = ada.followers(bobId, null, 1);
        assertThat(firstPage.userIds()).containsExactly(carolId);
        assertThat(firstPage.nextCursor()).isNotNull();

        // Ada unfollows Bob — she drops off his follower list, Bob drops off her following list.
        ada.unfollow(bobId);
        assertThat(drainFollowers(ada, bobId, null)).containsExactly(carolId);
        assertThat(drainFollowing(ada, adaId, null)).containsExactly(carolId);
    }

    @Test
    void oneBatchReadReportsTheViewersStandingWithEveryUserOnAListPage() {
        var ada = pictogram.registerViaGoogle("ada@example.com");
        String adaId = ada.completeOnboarding("ada_batch", "Ada", null).userId();
        var bob = pictogram.registerViaGoogle("bob@example.com");
        String bobId = bob.completeOnboarding("bob_batch", "Bob", null).userId();
        var carol = pictogram.registerViaGoogle("carol@example.com");
        String carolId = carol.completeOnboarding("carol_batch", "Carol", null).userId();

        // Ada follows Bob; Carol follows Bob; Bob follows Carol.
        ada.follow(bobId);
        carol.follow(bobId);
        bob.follow(carolId);

        // Ada opens Bob's follower list: one call resolves her standing with Bob and Carol.
        var standing = ada.followRelationships(bobId, carolId);

        assertThat(standing.get(bobId)).isEqualTo(new FollowRelationship(2, 1, true));
        assertThat(standing.get(carolId)).isEqualTo(new FollowRelationship(1, 1, false));
    }

    private static List<String> drainFollowers(Actor viewer, String userId, Integer limit) {
        return drain(cursor -> viewer.followers(userId, cursor, limit));
    }

    private static List<String> drainFollowing(Actor viewer, String userId, Integer limit) {
        return drain(cursor -> viewer.following(userId, cursor, limit));
    }

    /** Walks every page of a follow list, following the cursor, and returns the ids in order. */
    private static List<String> drain(Function<String, AccountPage> nextPage) {
        List<String> ids = new ArrayList<>();
        String cursor = null;
        do {
            AccountPage page = nextPage.apply(cursor);
            ids.addAll(page.userIds());
            cursor = page.nextCursor();
        } while (cursor != null);
        return ids;
    }
}
