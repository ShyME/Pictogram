package me.imshy.pictogram.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * The ticket's broad scenario: a user opens another user's profile by username and sees
 * their details; a username nobody holds comes back empty so the client can show a clear
 * not-found page. Written once against {@link PictogramApi}; green here through
 * {@link InProcessDriver}.
 */
class ViewProfileScenarioTest extends ScenarioTest {

    @Test
    void aUserOpensAnotherUsersProfileByUsernameAndSeesTheirDetails() {
        var ada = pictogram.registerViaGoogle("ada@example.com");
        ada.completeOnboarding("ada_lovelace", "Ada Lovelace", "Countess of Lovelace");

        var bob = pictogram.registerViaGoogle("bob@example.com");

        var seen = bob.viewProfile("ada_lovelace");

        assertThat(seen).hasValueSatisfying(profile -> {
            assertThat(profile.username()).isEqualTo("ada_lovelace");
            assertThat(profile.displayName()).isEqualTo("Ada Lovelace");
            assertThat(profile.bio()).isEqualTo("Countess of Lovelace");
        });
    }

    @Test
    void openingAProfileForAUsernameNobodyHoldsComesBackEmpty() {
        var ada = pictogram.registerViaGoogle("ada@example.com");

        assertThat(ada.viewProfile("ghost_user")).isEmpty();
    }
}
