package me.imshy.pictogram.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * The ticket's broad scenario: a user renames themselves; the old handle becomes reusable
 * by another user, and the old {@code /u/&lt;old&gt;} link stops resolving. Written once
 * against {@link PictogramApi}; green here through {@link InProcessDriver}.
 */
class EditProfileScenarioTest extends ScenarioTest {

    @Test
    void aRenameFreesTheOldHandleForAnotherUserAndBreaksTheOldLink() {
        var ada = pictogram.registerViaGoogle("ada@example.com");
        ada.completeOnboarding("ada", "Ada", "mathematician");

        var renamed = ada.editProfile("ada_lovelace", "Ada Lovelace", "Countess of Lovelace");
        assertThat(renamed.username()).isEqualTo("ada_lovelace");
        assertThat(renamed.displayName()).isEqualTo("Ada Lovelace");
        assertThat(renamed.bio()).isEqualTo("Countess of Lovelace");

        // The old link is dead...
        assertThat(ada.viewProfile("ada")).isEmpty();
        // ...and a second user can take the freed handle.
        var bob = pictogram.registerViaGoogle("bob@example.com");
        bob.completeOnboarding("ada", "Bob", null);

        assertThat(bob.viewProfile("ada")).hasValueSatisfying(profile ->
                assertThat(profile.displayName()).isEqualTo("Bob"));
        assertThat(ada.viewProfile("ada_lovelace")).hasValueSatisfying(profile ->
                assertThat(profile.displayName()).isEqualTo("Ada Lovelace"));
    }

    @Test
    void aUserEditsEachFieldOfTheirProfile() {
        var ada = pictogram.registerViaGoogle("ada@example.com");
        ada.completeOnboarding("ada");

        ada.editProfile("ada", "Ada Lovelace", null);
        assertThat(ada.currentProfile()).hasValueSatisfying(profile -> {
            assertThat(profile.username()).isEqualTo("ada");
            assertThat(profile.displayName()).isEqualTo("Ada Lovelace");
            assertThat(profile.bio()).isNull();
        });

        ada.editProfile("ada", "Ada Lovelace", "Writer of the first algorithm");
        assertThat(ada.currentProfile()).hasValueSatisfying(profile ->
                assertThat(profile.bio()).isEqualTo("Writer of the first algorithm"));
    }
}
