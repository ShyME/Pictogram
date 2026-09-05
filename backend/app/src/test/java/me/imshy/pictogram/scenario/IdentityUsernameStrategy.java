package me.imshy.pictogram.scenario;

import me.imshy.pictogram.scenario.PictogramApp.Profile;

/**
 * {@link UsernameStrategy} for the in-process transport, whose fresh database
 * never collides.
 */
final class IdentityUsernameStrategy implements UsernameStrategy {

    @Override
    public String qualify(String username) {
        return username;
    }

    @Override
    public Profile strip(Profile profile) {
        return profile;
    }
}
