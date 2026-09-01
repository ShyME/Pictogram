package me.imshy.pictogram.identity.internal.web;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Duration;
import me.imshy.pictogram.identity.internal.AuthProperties;
import me.imshy.pictogram.identity.internal.IdentityAuthentication;
import me.imshy.pictogram.identity.internal.InvalidRefreshTokenException;
import me.imshy.pictogram.identity.internal.Session;
import me.imshy.pictogram.shared.http.ProblemType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The session endpoints the SPA drives. Both authenticate purely by the refresh cookie, so
 * they sit outside the resource-server filter chain.
 */
@RestController
@RequestMapping("/api/auth")
class AuthController {

    private static final ProblemType SESSION_INVALID =
            new ProblemType("session-invalid", "Your session is no longer valid");

    private final IdentityAuthentication authentication;
    private final AuthProperties properties;
    private final Clock clock;

    AuthController(IdentityAuthentication authentication, AuthProperties properties, Clock clock) {
        this.authentication = authentication;
        this.properties = properties;
        this.clock = clock;
    }

    record AccessTokenResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String accessToken,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long expiresInSeconds) {
    }

    /** Rotates the refresh cookie and returns a fresh access token; reuse ends the session. */
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "A fresh access token; the refresh cookie is rotated.",
                content = @Content(schema = @Schema(implementation = AccessTokenResponse.class))),
        @ApiResponse(responseCode = "401", description = "The refresh cookie is missing, already used, or expired.",
                content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/refresh")
    ResponseEntity<?> refresh(HttpServletRequest request) {
        String presented = RefreshCookie.readFrom(request).orElse(null);
        if (presented == null) {
            return sessionInvalid();
        }
        Session session;
        try {
            session = authentication.refresh(presented);
        } catch (InvalidRefreshTokenException invalid) {
            return sessionInvalid();
        }
        long expiresIn = Duration.between(clock.instant(), session.accessTokenExpiresAt()).toSeconds();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session).toString())
                .body(new AccessTokenResponse(session.accessToken(), expiresIn));
    }

    /**
     * Ends the session: revokes the refresh-token family and clears the cookie. This chain
     * is stateless and has no servlet session to invalidate — the OIDC handshake session is
     * torn down at sign-in by {@code OidcSignInSuccessHandler}, not here (#27).
     */
    @ApiResponse(responseCode = "204", description = "The session is ended and the refresh cookie cleared.")
    @PostMapping("/logout")
    ResponseEntity<Void> logout(HttpServletRequest request) {
        RefreshCookie.readFrom(request).ifPresent(authentication::signOut);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, RefreshCookie.expired(properties.cookieSecure()).toString())
                .build();
    }

    private ResponseCookie refreshCookie(Session session) {
        return RefreshCookie.issue(session.refreshToken(), properties.refreshTokenTtl(), properties.cookieSecure());
    }

    private ResponseEntity<ProblemDetail> sessionInvalid() {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "Sign in again to continue.");
        problem.setType(SESSION_INVALID.uri());
        problem.setTitle(SESSION_INVALID.title());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .header(HttpHeaders.SET_COOKIE, RefreshCookie.expired(properties.cookieSecure()).toString())
                .body(problem);
    }
}
