package me.imshy.pictogram.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

interface EditProfileScenarios extends PictogramScenario {

    @Test
    default void aRenameFreesTheOldHandleForAnotherUserAndBreaksTheOldLink() {
        var ada = pictogram().registerViaGoogle("ada@example.com");
        ada.completeOnboarding("ada", "Ada", "mathematician");

        var renamed = ada.editProfile("ada_lovelace", "Ada Lovelace", "Countess of Lovelace");
        assertThat(renamed.username()).isEqualTo("ada_lovelace");
        assertThat(renamed.displayName()).isEqualTo("Ada Lovelace");
        assertThat(renamed.bio()).isEqualTo("Countess of Lovelace");

        assertThat(ada.viewProfile("ada")).isEmpty();
        var bob = pictogram().registerViaGoogle("bob@example.com");
        bob.completeOnboarding("ada", "Bob", null);

        assertThat(bob.viewProfile("ada"))
                .hasValueSatisfying(profile -> assertThat(profile.displayName()).isEqualTo("Bob"));
        assertThat(ada.viewProfile("ada_lovelace"))
                .hasValueSatisfying(profile -> assertThat(profile.displayName()).isEqualTo("Ada Lovelace"));
    }

    @Test
    default void aUserEditsEachFieldOfTheirProfile() {
        var ada = pictogram().registerViaGoogle("ada@example.com");
        ada.completeOnboarding("ada");

        ada.editProfile("ada", "Ada Lovelace", null);
        assertThat(ada.currentProfile()).hasValueSatisfying(profile -> {
            assertThat(profile.username()).isEqualTo("ada");
            assertThat(profile.displayName()).isEqualTo("Ada Lovelace");
            assertThat(profile.bio()).isNull();
        });

        ada.editProfile("ada", "Ada Lovelace", "Writer of the first algorithm");
        assertThat(ada.currentProfile())
                .hasValueSatisfying(profile -> assertThat(profile.bio()).isEqualTo("Writer of the first algorithm"));
    }
}
