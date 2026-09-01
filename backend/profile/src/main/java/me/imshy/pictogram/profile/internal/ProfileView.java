package me.imshy.pictogram.profile.internal;

import me.imshy.pictogram.shared.UserId;

public record ProfileView(UserId userId, String username, String displayName, String bio) {

    static ProfileView of(Profile profile) {
        return new ProfileView(profile.userId(), profile.username(), profile.displayName(), profile.bio());
    }
}
