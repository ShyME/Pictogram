package me.imshy.pictogram.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

interface UserOnboardingTests extends AppUnderTest {

    @Test
    default void newUserSignsInPicksAUsernameAndLandsOnAnEmptyFeed() {
        var ada = pictogram().registerViaGoogle("ada@example.com");

        assertThat(ada.currentProfile()).isEmpty();

        var profile = ada.completeOnboarding("ada_lovelace", "Ada Lovelace", "Countess of Lovelace");

        assertThat(profile.username()).isEqualTo("ada_lovelace");
        assertThat(profile.displayName()).isEqualTo("Ada Lovelace");
        assertThat(ada.currentProfile()).contains(profile);

        var feed = ada.openFeed();

        assertThat(feed.isEmpty()).isTrue();
    }
}
