package me.imshy.pictogram.scenario;

import me.imshy.pictogram.scenario.PictogramApi.Profile;

/**
 * How {@link HttpPictogramApi}'s actor handles the four username-carrying calls
 * ({@code completeOnboarding}, {@code editProfile}, {@code viewProfile}, and the {@code Profile} that
 * every one of those plus {@code currentProfile} returns). In the same shape as {@link SignIn}: one
 * seam, injected per transport.
 */
interface UsernamePolicy {

    String qualify(String username);

    Profile strip(Profile profile);
}
