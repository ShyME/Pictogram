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

    public ProfileView completeOnboarding(UserId user, String username, String displayName, String bio) {
        Username handle = new Username(username);
        DisplayName name = DisplayName.of(displayName);
        Bio about = Bio.of(bio);

        if (profiles.existsById(user.value())) {
            throw new AlreadyOnboardedException();
        }
        if (profiles.existsByUsername(handle.value())) {
            throw new UsernameAlreadyTakenException();
        }

        var profile = Profile.onboard(user, handle, name, about, clock.instant());
        try {
            profiles.save(profile);
        } catch (DataIntegrityViolationException lostARace) {
            throw profiles.existsById(user.value())
                    ? new AlreadyOnboardedException()
                    : new UsernameAlreadyTakenException();
        }
        return ProfileView.of(profile);
    }

    public ProfileView profileOf(UserId user) {
        return profiles.findById(user.value())
                .map(ProfileView::of)
                .orElseThrow(ProfileNotFoundException::new);
    }
}
