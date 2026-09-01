package me.imshy.pictogram.profile.internal;

import java.time.Clock;
import me.imshy.pictogram.profile.ProfileUpdated;
import me.imshy.pictogram.shared.UserId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class ProfileEditing {

    private final Profiles profiles;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    ProfileEditing(Profiles profiles, ApplicationEventPublisher events, Clock clock) {
        this.profiles = profiles;
        this.events = events;
        this.clock = clock;
    }

    public ProfileView editProfile(UserId user, String username, String displayName, String bio) {
        Username handle = new Username(username);
        DisplayName name = DisplayName.of(displayName);
        Bio about = Bio.of(bio);

        Profile profile = profiles.findById(user.value())
                .orElseThrow(ProfileNotFoundException::new);

        boolean renaming = !handle.value().equals(profile.username());
        if (renaming && profiles.existsByUsername(handle.value())) {
            throw new UsernameAlreadyTakenException();
        }

        if (!profile.edit(handle, name, about)) {
            return ProfileView.of(profile);
        }

        Profile saved;
        try {
            saved = profiles.save(profile);
        } catch (DataIntegrityViolationException lostARace) {
            throw new UsernameAlreadyTakenException();
        }

        events.publishEvent(new ProfileUpdated(user, saved.username(), clock.instant()));
        return ProfileView.of(saved);
    }
}
