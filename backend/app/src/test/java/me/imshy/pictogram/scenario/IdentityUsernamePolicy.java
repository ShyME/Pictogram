package me.imshy.pictogram.scenario;

import me.imshy.pictogram.scenario.PictogramApi.Profile;

/** {@link UsernamePolicy} for the in-process transport, whose fresh database never collides. */
final class IdentityUsernamePolicy implements UsernamePolicy {

    @Override
    public String qualify(String username) {
        return username;
    }

    @Override
    public Profile strip(Profile profile) {
        return profile;
    }
}
