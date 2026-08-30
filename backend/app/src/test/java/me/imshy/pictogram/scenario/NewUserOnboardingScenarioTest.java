package me.imshy.pictogram.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * The ticket's broad scenario: a new user signs in with Google, picks a username, and
 * lands on an empty feed. Written once against {@link PictogramApi}; green here through
 * {@link InProcessDriver}.
 */
class NewUserOnboardingScenarioTest extends ScenarioTest {

    @Test
    void newUserSignsInPicksAUsernameAndLandsOnAnEmptyFeed() {
        var ada = pictogram.registerViaGoogle("ada@example.com");

        // Signing in does not create a profile — the user is "not yet onboarded".
        assertThat(ada.currentProfile()).isEmpty();

        var profile = ada.completeOnboarding("ada_lovelace", "Ada Lovelace", "Countess of Lovelace");

        assertThat(profile.username()).isEqualTo("ada_lovelace");
        assertThat(profile.displayName()).isEqualTo("Ada Lovelace");
        assertThat(ada.currentProfile()).contains(profile);

        var feed = ada.openFeed();

        assertThat(feed.isEmpty()).isTrue();
    }
}
