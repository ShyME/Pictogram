package me.imshy.pictogram.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import me.imshy.pictogram.identity.IdentityStats;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class IdentityStatsServiceTest extends ClockControlledModuleTest {

    @Autowired
    IdentityAuthentication authentication;

    @Autowired
    IdentityStats identityStats;

    @Test
    void totalUsersCountsEveryRegisteredUserOnce() {
        authentication.authenticate(new ExternalAccount("google", "sub-total-1", "ada@example.com"));
        authentication.authenticate(new ExternalAccount("google", "sub-total-2", "bob@example.com"));
        authentication.authenticate(new ExternalAccount("google", "sub-total-2", "bob@example.com"));

        assertThat(identityStats.totalUsers()).isEqualTo(2);
    }

    @Test
    void activeSinceCountsAUserWhoseTokenWasIssuedAtOrAfterTheCutoff() {
        authentication.authenticate(new ExternalAccount("google", "sub-active-1", "ada@example.com"));

        assertThat(identityStats.activeSince(time.instant())).isEqualTo(1);
    }

    @Test
    void activeSinceCountsAUserWhoseTokenWasRotatedSinceTheCutoffEvenIfIssuedBeforeIt() {
        var session = authentication.authenticate(new ExternalAccount("google", "sub-active-2", "grace@example.com"));

        time.advance(Duration.ofHours(20));
        var cutoff = time.instant();
        time.advance(Duration.ofHours(1));
        authentication.refresh(session.refreshToken());

        assertThat(identityStats.activeSince(cutoff)).isEqualTo(1);
    }

    @Test
    void activeSinceExcludesAUserWithNoActivitySinceTheCutoff() {
        authentication.authenticate(new ExternalAccount("google", "sub-active-3", "lin@example.com"));

        time.advance(Duration.ofHours(25));

        assertThat(identityStats.activeSince(time.instant())).isZero();
    }
}
