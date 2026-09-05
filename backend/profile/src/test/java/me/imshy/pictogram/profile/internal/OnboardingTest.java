package me.imshy.pictogram.profile.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Instant;
import me.imshy.pictogram.shared.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class OnboardingTest extends ProfileModuleIntegrationTest {

    @Autowired
    Onboarding onboarding;

    @Autowired
    Profiles profiles;

    @Test
    void pickingAUsernameCreatesTheProfile() {
        var user = UserId.random();

        var created = onboarding.completeOnboarding(user, "ada_lovelace", "Ada Lovelace", "The first programmer");

        assertThat(created.userId()).isEqualTo(user);
        assertThat(created.username()).isEqualTo("ada_lovelace");
        assertThat(created.displayName()).isEqualTo("Ada Lovelace");
        assertThat(created.bio()).isEqualTo("The first programmer");
        assertThat(onboarding.profileOf(user)).isEqualTo(created);
    }

    @Test
    void displayNameAndBioAreOptional() {
        var user = UserId.random();

        var created = onboarding.completeOnboarding(user, "grace", "  ", null);

        assertThat(created.displayName()).isNull();
        assertThat(created.bio()).isNull();
    }

    @Test
    void aUserWithoutAProfileHasNotYetOnboarded() {
        assertThatExceptionOfType(ProfileNotFoundException.class)
            .isThrownBy(() -> onboarding.profileOf(UserId.random()));
    }

    @Test
    void aUsernameCannotBeTakenTwice() {
        onboarding.completeOnboarding(UserId.random(), "ada", null, null);

        assertThatExceptionOfType(UsernameAlreadyTakenException.class)
            .isThrownBy(() -> onboarding.completeOnboarding(UserId.random(), "ada", null, null));
    }

    @Test
    void onboardingAUserWhoAlreadyHasAProfileIsRejected() {
        var user = UserId.random();
        onboarding.completeOnboarding(user, "ada", null, null);

        assertThatExceptionOfType(AlreadyOnboardedException.class)
            .isThrownBy(() -> onboarding.completeOnboarding(user, "ada_again", null, null));
    }

    @Test
    void anOverlongBioIsRejected() {
        assertThatExceptionOfType(InvalidProfileDetailsException.class).isThrownBy(
            () -> onboarding.completeOnboarding(UserId.random(), "ada", null, "x".repeat(Bio.MAX_LENGTH + 1)));
    }

    @Test
    void anEmojiCountsAsOneCharacterNotTwo() {
        var created = onboarding.completeOnboarding(UserId.random(), "ada", "🎨".repeat(DisplayName.MAX_LENGTH), null);

        assertThat(created.displayName()).hasSize(DisplayName.MAX_LENGTH * 2);
    }

    @Test
    void aProfileRowIsInsertOnlySoARacingSecondOnboardingCannotOverwriteIt() {
        var user = UserId.random();
        onboarding.completeOnboarding(user, "ada", "Ada", null);

        var racing = Profile.onboard(user, new Username("mallory"), DisplayName.of(null), Bio.of(null),
            Instant.parse("2026-08-31T00:00:00Z"));
        assertThatExceptionOfType(DataIntegrityViolationException.class).isThrownBy(() -> profiles.save(racing));

        assertThat(onboarding.profileOf(user).username()).isEqualTo("ada");
    }
}
