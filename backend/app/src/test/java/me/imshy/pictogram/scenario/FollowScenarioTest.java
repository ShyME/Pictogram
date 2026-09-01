package me.imshy.pictogram.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import me.imshy.pictogram.scenario.PictogramApi.FollowOutcome;
import org.junit.jupiter.api.Test;

class FollowScenarioTest extends ScenarioTest {

    @Test
    void followThenUnfollowWithIdempotentRepeatsAndSelfFollowRejected() {
        var ada = pictogram.registerViaGoogle("ada@example.com");
        var adaProfile = ada.completeOnboarding("ada_lovelace", "Ada Lovelace", null);
        var bob = pictogram.registerViaGoogle("bob@example.com");
        var bobProfile = bob.completeOnboarding("bob_ross", "Bob Ross", null);

        assertThat(ada.follow(bobProfile.userId())).isEqualTo(FollowOutcome.OK);
        assertThat(ada.follow(bobProfile.userId())).isEqualTo(FollowOutcome.OK);

        var bobStanding = ada.followRelationship(bobProfile.userId());
        assertThat(bobStanding.followerCount()).isEqualTo(1);
        assertThat(bobStanding.followedByViewer()).isTrue();
        assertThat(ada.followRelationship(adaProfile.userId()).followingCount()).isEqualTo(1);
        assertThat(bob.followRelationship(adaProfile.userId()).followedByViewer())
                .isFalse();

        ada.unfollow(bobProfile.userId());
        ada.unfollow(bobProfile.userId());

        var afterUnfollow = ada.followRelationship(bobProfile.userId());
        assertThat(afterUnfollow.followerCount()).isZero();
        assertThat(afterUnfollow.followedByViewer()).isFalse();
        assertThat(ada.followRelationship(adaProfile.userId()).followingCount()).isZero();

        assertThat(ada.follow(adaProfile.userId())).isEqualTo(FollowOutcome.SELF_FOLLOW);
    }
}
