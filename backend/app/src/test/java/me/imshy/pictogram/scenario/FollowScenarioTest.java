package me.imshy.pictogram.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import me.imshy.pictogram.scenario.PictogramApi.FollowOutcome;
import org.junit.jupiter.api.Test;

/**
 * The ticket's broad scenario: from a profile, one user follows another and the counts
 * move; unfollowing moves them back; repeating either command is a harmless no-op; and a
 * user cannot follow themselves. Written once against {@link PictogramApi}; green here
 * through {@link InProcessDriver}.
 */
class FollowScenarioTest extends ScenarioTest {

    @Test
    void followThenUnfollowWithIdempotentRepeatsAndSelfFollowRejected() {
        var ada = pictogram.registerViaGoogle("ada@example.com");
        var adaProfile = ada.completeOnboarding("ada_lovelace", "Ada Lovelace", null);
        var bob = pictogram.registerViaGoogle("bob@example.com");
        var bobProfile = bob.completeOnboarding("bob_ross", "Bob Ross", null);

        assertThat(ada.follow(bobProfile.userId())).isEqualTo(FollowOutcome.OK);
        // Following again changes nothing.
        assertThat(ada.follow(bobProfile.userId())).isEqualTo(FollowOutcome.OK);

        var bobStanding = ada.followRelationship(bobProfile.userId());
        assertThat(bobStanding.followerCount()).isEqualTo(1);
        assertThat(bobStanding.followedByViewer()).isTrue();
        assertThat(ada.followRelationship(adaProfile.userId()).followingCount()).isEqualTo(1);
        // Bob does not see Ada as someone he follows.
        assertThat(bob.followRelationship(adaProfile.userId()).followedByViewer()).isFalse();

        ada.unfollow(bobProfile.userId());
        // Unfollowing again changes nothing.
        ada.unfollow(bobProfile.userId());

        var afterUnfollow = ada.followRelationship(bobProfile.userId());
        assertThat(afterUnfollow.followerCount()).isZero();
        assertThat(afterUnfollow.followedByViewer()).isFalse();
        assertThat(ada.followRelationship(adaProfile.userId()).followingCount()).isZero();

        assertThat(ada.follow(adaProfile.userId())).isEqualTo(FollowOutcome.SELF_FOLLOW);
    }
}
