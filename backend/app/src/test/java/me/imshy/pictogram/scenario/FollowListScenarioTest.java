package me.imshy.pictogram.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import me.imshy.pictogram.scenario.PictogramApi.AccountPage;
import me.imshy.pictogram.scenario.PictogramApi.Actor;
import me.imshy.pictogram.scenario.PictogramApi.FollowRelationship;
import org.junit.jupiter.api.Test;

class FollowListScenarioTest extends ScenarioTest {

    @Test
    void followerAndFollowingListsReadBackNewestFirstPageCleanlyAndTrackUnfollows() {
        var ada = pictogram.registerViaGoogle("ada@example.com");
        String adaId = ada.completeOnboarding("ada_lists", "Ada", null).userId();
        var bob = pictogram.registerViaGoogle("bob@example.com");
        String bobId = bob.completeOnboarding("bob_lists", "Bob", null).userId();
        var carol = pictogram.registerViaGoogle("carol@example.com");
        String carolId = carol.completeOnboarding("carol_lists", "Carol", null).userId();

        ada.follow(bobId);
        ada.follow(carolId);
        carol.follow(bobId);

        assertThat(drainFollowers(ada, bobId, null)).containsExactly(carolId, adaId);
        assertThat(drainFollowing(ada, adaId, null)).containsExactly(carolId, bobId);
        assertThat(drainFollowing(carol, carolId, null)).containsExactly(bobId);

        assertThat(drainFollowers(ada, bobId, 1)).containsExactly(carolId, adaId);
        AccountPage firstPage = ada.followers(bobId, null, 1);
        assertThat(firstPage.userIds()).containsExactly(carolId);
        assertThat(firstPage.nextCursor()).isNotNull();

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

        ada.follow(bobId);
        carol.follow(bobId);
        bob.follow(carolId);

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
