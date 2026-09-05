package me.imshy.pictogram.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Duration;
import me.imshy.pictogram.identity.InvalidAccessTokenException;
import me.imshy.pictogram.identity.PictogramAccessTokens;
import me.imshy.pictogram.identity.UserRegistered;
import me.imshy.pictogram.shared.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.AssertablePublishedEvents;

class IdentitySignInScenarioTest extends ClockControlledModuleTest {

    @Autowired
    IdentityAuthentication authentication;

    @Autowired
    AppUsers users;

    @Autowired
    PictogramAccessTokens accessTokens;

    @Test
    void firstSignInRegistersTheUserAndAnnouncesIt(AssertablePublishedEvents events) {
        var google = new ExternalAccount("google", "google-sub-001", "ada@example.com");

        var session = authentication.authenticate(google);

        UserId registered = accessTokens.resolve(session.accessToken());
        assertThat(users.findByProviderAndSubject("google", "google-sub-001")).get().extracting(AppUser::userId)
            .isEqualTo(registered);
        assertThat(events.ofType(UserRegistered.class)).singleElement().satisfies(event -> {
            assertThat(event.userId()).isEqualTo(registered);
            assertThat(event.email()).isEqualTo("ada@example.com");
        });
    }

    @Test
    void aReturningPersonResolvesToTheSameUserWithNoSecondAnnouncement(AssertablePublishedEvents events) {
        var google = new ExternalAccount("google", "google-sub-002", "grace@example.com");
        UserId first = accessTokens.resolve(authentication.authenticate(google).accessToken());

        UserId second = accessTokens.resolve(authentication.authenticate(google).accessToken());

        assertThat(second).isEqualTo(first);
        assertThat(events.ofType(UserRegistered.class)).hasSize(1);
    }

    @Test
    void anExpiredAccessTokenIsExchangedForAFreshOneViaRefresh() {
        var session = authentication.authenticate(new ExternalAccount("google", "google-sub-003", "lin@example.com"));
        UserId user = accessTokens.resolve(session.accessToken());

        time.advance(Duration.ofMinutes(20));
        assertThatExceptionOfType(InvalidAccessTokenException.class)
            .isThrownBy(() -> accessTokens.resolve(session.accessToken()));

        var refreshed = authentication.refresh(session.refreshToken());

        assertThat(accessTokens.resolve(refreshed.accessToken())).isEqualTo(user);
    }

    @Test
    void replayingARotatedRefreshTokenAfterTheGraceWindowKillsTheWholeChain() {
        var session = authentication.authenticate(new ExternalAccount("google", "google-sub-004", "mae@example.com"));
        var rotated = authentication.refresh(session.refreshToken());

        time.advance(Duration.ofMinutes(1).plusSeconds(1));

        assertThatExceptionOfType(InvalidRefreshTokenException.class)
            .isThrownBy(() -> authentication.refresh(session.refreshToken()));
        assertThatExceptionOfType(InvalidRefreshTokenException.class)
            .isThrownBy(() -> authentication.refresh(rotated.refreshToken()));
    }
}
