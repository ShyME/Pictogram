package me.imshy.pictogram.profile.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import me.imshy.pictogram.profile.ProfileUpdated;
import me.imshy.pictogram.shared.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.AssertablePublishedEvents;

class ProfileEditingTest extends ProfileModuleIntegrationTest {

    @Autowired
    ProfileEditing editing;

    @Autowired
    Onboarding onboarding;

    @Autowired
    ProfileDirectory directory;

    @Test
    void editsTheDisplayNameAndBio() {
        var user = UserId.random();
        onboarding.completeOnboarding(user, "ada", "Ada", "first programmer");

        var edited = editing.editProfile(user, "ada", "Ada Lovelace", "Countess of Lovelace");

        assertThat(edited.username()).isEqualTo("ada");
        assertThat(edited.displayName()).isEqualTo("Ada Lovelace");
        assertThat(edited.bio()).isEqualTo("Countess of Lovelace");
        assertThat(onboarding.profileOf(user)).isEqualTo(edited);
    }

    @Test
    void clearingTheDisplayNameAndBioLeavesThemUnset() {
        var user = UserId.random();
        onboarding.completeOnboarding(user, "ada", "Ada", "a bio");

        var edited = editing.editProfile(user, "ada", "  ", "");

        assertThat(edited.displayName()).isNull();
        assertThat(edited.bio()).isNull();
    }

    @Test
    void aRenameFreesTheOldHandleImmediately() {
        var ada = UserId.random();
        onboarding.completeOnboarding(ada, "ada", "Ada", null);

        editing.editProfile(ada, "ada_lovelace", "Ada", null);

        assertThatExceptionOfType(ProfileNotFoundException.class).isThrownBy(() -> directory.byUsername("ada"));
        assertThat(directory.byUsername("ada_lovelace").userId()).isEqualTo(ada);

        var mallory = UserId.random();
        onboarding.completeOnboarding(mallory, "ada", null, null);
        assertThat(directory.byUsername("ada").userId()).isEqualTo(mallory);
    }

    @Test
    void keepingTheSameUsernameIsNotTreatedAsARename() {
        var user = UserId.random();
        onboarding.completeOnboarding(user, "ada", "Ada", null);

        assertThat(editing.editProfile(user, "ada", "Ada L.", null).username()).isEqualTo("ada");
    }

    @Test
    void renamingToAHandleAnotherUserHoldsIsRejected() {
        var ada = UserId.random();
        onboarding.completeOnboarding(ada, "ada", null, null);
        onboarding.completeOnboarding(UserId.random(), "grace", null, null);

        assertThatExceptionOfType(UsernameAlreadyTakenException.class)
                .isThrownBy(() -> editing.editProfile(ada, "grace", null, null));
    }

    @Test
    void editingAProfileThatDoesNotExistIsNotFound() {
        assertThatExceptionOfType(ProfileNotFoundException.class)
                .isThrownBy(() -> editing.editProfile(UserId.random(), "nobody", null, null));
    }

    @Test
    void aMalformedNewUsernameIsRejected() {
        var user = UserId.random();
        onboarding.completeOnboarding(user, "ada", null, null);

        assertThatExceptionOfType(MalformedUsernameException.class)
                .isThrownBy(() -> editing.editProfile(user, "No Good", null, null));
    }

    @Test
    void anOverlongBioIsRejected() {
        var user = UserId.random();
        onboarding.completeOnboarding(user, "ada", null, null);

        assertThatExceptionOfType(InvalidProfileDetailsException.class)
                .isThrownBy(() -> editing.editProfile(user, "ada", null, "x".repeat(Bio.MAX_LENGTH + 1)));
    }

    @Test
    void anEditThatMovesAFieldAnnouncesProfileUpdated(AssertablePublishedEvents events) {
        var user = UserId.random();
        onboarding.completeOnboarding(user, "ada", null, null);

        editing.editProfile(user, "ada_lovelace", "Ada", null);

        assertThat(events.ofType(ProfileUpdated.class)).singleElement().satisfies(event -> {
            assertThat(event.userId()).isEqualTo(user);
            assertThat(event.username()).isEqualTo("ada_lovelace");
            assertThat(event.updatedAt()).isNotNull();
        });
    }

    @Test
    void aNoOpEditIsAcceptedButAnnouncesNothing(AssertablePublishedEvents events) {
        var user = UserId.random();
        var created = onboarding.completeOnboarding(user, "ada", "Ada", "a bio");

        assertThat(editing.editProfile(user, "ada", "Ada", "a bio")).isEqualTo(created);
        assertThat(events.ofType(ProfileUpdated.class)).isEmpty();
    }
}
