package me.imshy.pictogram.profile.internal.web;

import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import me.imshy.pictogram.profile.internal.Onboarding;
import me.imshy.pictogram.profile.internal.ProfileDirectory;
import me.imshy.pictogram.profile.internal.ProfileView;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.http.CurrentUser;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The profile endpoints. Two the SPA drives during onboarding — create a profile by
 * choosing a username, read the caller's own profile (404 until they have onboarded) — and
 * two public reads the client composes screens from (ADR-0005): a lookup by username for
 * the {@code /u/<username>} page, and a batch lookup by {@link UserId} set so a feed of
 * cards resolves its authors in one call rather than N.
 */
@RestController
@RequestMapping("/api/profiles")
class ProfilesController {

    private final Onboarding onboarding;
    private final ProfileDirectory directory;

    ProfilesController(Onboarding onboarding, ProfileDirectory directory) {
        this.onboarding = onboarding;
        this.directory = directory;
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

    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The batch of profiles for the ids that have one.",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProfileView.class))))
    })
    @GetMapping(params = "ids")
    List<ProfileView> byIds(@RequestParam("ids") Set<UUID> ids) {
        return directory.byIds(ids.stream().map(UserId::new).toList());
    }

    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The public profile for that username.",
                content = @Content(schema = @Schema(implementation = ProfileView.class))),
        @ApiResponse(responseCode = "404", description = "No user has that username.",
                content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{username}")
    ProfileView byUsername(@PathVariable("username") String username) {
        return directory.byUsername(username);
    }
}
