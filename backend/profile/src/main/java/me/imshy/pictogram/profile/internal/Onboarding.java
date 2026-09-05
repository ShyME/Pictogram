package me.imshy.pictogram.profile.internal;

import java.time.Clock;
import me.imshy.pictogram.shared.UserId;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class Onboarding {

    private final Profiles profiles;
    private final Clock clock;

    Onboarding(Profiles profiles, Clock clock) {
        this.profiles = profiles;
        this.clock = clock;
    }

    public ProfileView completeOnboarding(UserId userId, String username, String displayName, String bio) {
        Username usernameHandle = new Username(username);
        DisplayName displayNameHandle = DisplayName.of(displayName);
        Bio bioHandle = Bio.of(bio);

        if (profiles.existsById(userId.value())) {
            throw new AlreadyOnboardedException();
        }
        if (profiles.existsByUsername(usernameHandle.value())) {
            throw new UsernameAlreadyTakenException();
        }

        var profile = Profile.onboard(userId, usernameHandle, displayNameHandle, bioHandle, clock.instant());
        try {
            profiles.save(profile);
        } catch (DataIntegrityViolationException lostARace) {
            throw profiles.existsById(userId.value())
                ? new AlreadyOnboardedException()
                : new UsernameAlreadyTakenException();
        }
        return ProfileView.of(profile);
    }

    public ProfileView profileOf(UserId user) {
        return profiles.findById(user.value()).map(ProfileView::of).orElseThrow(ProfileNotFoundException::new);
    }
}
