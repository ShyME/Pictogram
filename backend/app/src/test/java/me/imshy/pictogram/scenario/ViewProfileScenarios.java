package me.imshy.pictogram.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

interface ViewProfileScenarios extends PictogramScenario {

    @Test
    default void aUserOpensAnotherUsersProfileByUsernameAndSeesTheirDetails() {
        var ada = pictogram().registerViaGoogle("ada@example.com");
        ada.completeOnboarding("ada_lovelace", "Ada Lovelace", "Countess of Lovelace");

        var bob = pictogram().registerViaGoogle("bob@example.com");

        var seen = bob.viewProfile("ada_lovelace");

        assertThat(seen).hasValueSatisfying(profile -> {
            assertThat(profile.username()).isEqualTo("ada_lovelace");
            assertThat(profile.displayName()).isEqualTo("Ada Lovelace");
            assertThat(profile.bio()).isEqualTo("Countess of Lovelace");
        });
    }

    @Test
    default void openingAProfileForAUsernameNobodyHoldsComesBackEmpty() {
        var ada = pictogram().registerViaGoogle("ada@example.com");

        assertThat(ada.viewProfile("ghost_user")).isEmpty();
    }
}
