package me.imshy.pictogram.profile.internal.web;

import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.net.URI;
import me.imshy.pictogram.profile.internal.Onboarding;
import me.imshy.pictogram.profile.internal.ProfileView;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.http.CurrentUser;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
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

    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "The profile was created.",
                headers = @Header(name = "Location", description = "The new profile's URL, by username.",
                        schema = @Schema(type = "string")),
                content = @Content(schema = @Schema(implementation = ProfileView.class))),
        @ApiResponse(responseCode = "400", description = "The username is malformed, or the display name or bio is invalid.",
                content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "The username is taken, or this account has already onboarded.",
                content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    ResponseEntity<ProfileView> onboard(@CurrentUser UserId user, @RequestBody OnboardingRequest request) {
        ProfileView profile = onboarding.completeOnboarding(
                user, request.username(), request.displayName(), request.bio());
        return ResponseEntity.created(URI.create("/api/profiles/" + profile.username())).body(profile);
    }

    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The caller's own profile.",
                content = @Content(schema = @Schema(implementation = ProfileView.class))),
        @ApiResponse(responseCode = "404", description = "The caller has no profile yet — they have not onboarded.",
                content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/me")
    ProfileView me(@CurrentUser UserId user) {
        return onboarding.profileOf(user);
    }
}
