package me.imshy.pictogram.profile.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;
import me.imshy.pictogram.shared.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ProfileDirectoryTest extends ProfileModuleIntegrationTest {

    @Autowired
    ProfileDirectory directory;

    @Autowired
    Onboarding onboarding;

    @Test
    void looksUpAProfileByUsername() {
        var ada = UserId.random();
        onboarding.completeOnboarding(ada, "ada_lovelace", "Ada Lovelace", "Countess of Lovelace");

        var found = directory.byUsername("ada_lovelace");

        assertThat(found.userId()).isEqualTo(ada);
        assertThat(found.username()).isEqualTo("ada_lovelace");
        assertThat(found.displayName()).isEqualTo("Ada Lovelace");
        assertThat(found.bio()).isEqualTo("Countess of Lovelace");
    }

    @Test
    void aUsernameLookupIgnoresCasingSinceHandlesAreStoredLowercase() {
        onboarding.completeOnboarding(UserId.random(), "ada_lovelace", "Ada", null);

        assertThat(directory.byUsername("Ada_Lovelace").username()).isEqualTo("ada_lovelace");
    }

    @Test
    void anUnknownUsernameIsNotFound() {
        assertThatExceptionOfType(ProfileNotFoundException.class).isThrownBy(() -> directory.byUsername("nobody_here"));
    }

    @Test
    void batchLookupReturnsAProfilePerKnownIdAndOmitsTheRest() {
        var ada = UserId.random();
        var grace = UserId.random();
        var missing = UserId.random();
        onboarding.completeOnboarding(ada, "ada", null, null);
        onboarding.completeOnboarding(grace, "grace", null, null);

        var profiles = directory.byIds(List.of(ada, missing, grace));

        assertThat(profiles).extracting(ProfileView::username).containsExactlyInAnyOrder("ada", "grace");
    }

    @Test
    void batchLookupOfNothingIsEmpty() {
        assertThat(directory.byIds(List.of())).isEmpty();
    }
}
