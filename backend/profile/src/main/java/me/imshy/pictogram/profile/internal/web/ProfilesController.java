package me.imshy.pictogram.profile.internal.web;

import java.net.URI;
import me.imshy.pictogram.profile.internal.Onboarding;
import me.imshy.pictogram.profile.internal.ProfileView;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.http.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The profile endpoints the SPA drives during onboarding: create a profile by choosing a
 * username, and read the caller's own profile (404 until they have onboarded). The public
 * profile-read endpoints — by username, batch by id — arrive with #11.
 */
@RestController
@RequestMapping("/api/profiles")
class ProfilesController {

    private final Onboarding onboarding;

    ProfilesController(Onboarding onboarding) {
        this.onboarding = onboarding;
    }

    record OnboardingRequest(String username, String displayName, String bio) {
    }

    @PostMapping
    ResponseEntity<ProfileView> onboard(@CurrentUser UserId user, @RequestBody OnboardingRequest request) {
        ProfileView profile = onboarding.completeOnboarding(
                user, request.username(), request.displayName(), request.bio());
        return ResponseEntity.created(URI.create("/api/profiles/" + profile.username())).body(profile);
    }

    @GetMapping("/me")
    ProfileView me(@CurrentUser UserId user) {
        return onboarding.profileOf(user);
    }
}
