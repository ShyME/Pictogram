package me.imshy.pictogram.profile.internal;

import me.imshy.pictogram.shared.UserId;

/**
 * A read of a {@link Profile} for the module's web layer. {@code displayName} and
 * {@code bio} are {@code null} when not set. The public profile-read model that other
 * contexts consume arrives with the profile-read endpoints (#11).
 */
public record ProfileView(UserId userId, String username, String displayName, String bio) {

    static ProfileView of(Profile profile) {
        return new ProfileView(profile.userId(), profile.username(), profile.displayName(), profile.bio());
    }
}
