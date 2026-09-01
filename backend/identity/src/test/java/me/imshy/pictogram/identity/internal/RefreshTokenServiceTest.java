package me.imshy.pictogram.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import me.imshy.pictogram.identity.internal.refreshtoken.RefreshTokenService;
import me.imshy.pictogram.shared.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class RefreshTokenServiceTest extends ClockControlledModuleTest {

    /** Matches {@code pictogram.auth.refresh-token-ttl}'s default. */
    private static final Duration TTL = Duration.ofDays(30);

    /** Matches {@code pictogram.auth.refresh-token-rotation-grace}'s default. */
    private static final Duration GRACE = Duration.ofSeconds(60);

    @Autowired
    RefreshTokenService refreshTokens;

    @Autowired
    PlatformTransactionManager txManager;

    @Test
    void rotationIssuesANewTokenAndSpendsTheOldOne() {
        var user = UserId.random();
        var first = refreshTokens.startSession(user);

        var second = refreshTokens.rotate(first.token());

        assertThat(second.token()).isNotEqualTo(first.token());
        assertThat(second.user()).isEqualTo(user);
        assertThatExceptionOfType(InvalidRefreshTokenException.class)
                .isThrownBy(() -> refreshTokens.rotate(first.token()));
    }

    @Test
    void replayingASpentTokenAfterTheGraceWindowRevokesTheWholeFamily() {
        var first = refreshTokens.startSession(UserId.random());
        var second = refreshTokens.rotate(first.token());

        time.advance(GRACE.plusSeconds(1));

        assertThatExceptionOfType(RefreshTokenReuseException.class)
                .isThrownBy(() -> refreshTokens.rotate(first.token()));

        // the still-"current" token is dead too — the chain is gone, back to sign-in
        assertThatExceptionOfType(InvalidRefreshTokenException.class)
                .isThrownBy(() -> refreshTokens.rotate(second.token()));
    }

    @Test
    void replayingASpentTokenWithinTheGraceWindowIsRejectedButLeavesTheFamilyIntact() {
        var first = refreshTokens.startSession(UserId.random());
        var second = refreshTokens.rotate(first.token());

        assertThatExceptionOfType(InvalidRefreshTokenException.class)
                .isThrownBy(() -> refreshTokens.rotate(first.token()))
                .isNotInstanceOf(RefreshTokenReuseException.class);

        // the legitimate current token still rotates — a benign double-submit is not a logout
        assertThatNoException().isThrownBy(() -> refreshTokens.rotate(second.token()));
    }

    @Test
    void twoRefreshesRacingOnOneTokenNeverBothSucceedAndNeitherEndsTheSession() throws Exception {
        var first = refreshTokens.startSession(UserId.random());
        var barrier = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Callable<Object> rotate = () -> {
            barrier.await();
            try {
                return refreshTokens.rotate(first.token());
            } catch (RuntimeException rejected) {
                return rejected;
            }
        };

        List<Future<Object>> attempts = pool.invokeAll(List.of(rotate, rotate));
        pool.shutdown();

        var outcomes = attempts.stream().map(RefreshTokenServiceTest::valueOf).toList();
        var winner = outcomes.stream()
                .filter(RefreshTokenService.Issued.class::isInstance)
                .map(RefreshTokenService.Issued.class::cast)
                .toList();
        assertThat(winner).hasSize(1);
        assertThat(outcomes).anySatisfy(outcome -> assertThat(outcome)
                .isInstanceOf(InvalidRefreshTokenException.class)
                .isNotInstanceOf(RefreshTokenReuseException.class));
        // the racer that won already set the next cookie; it must still be usable
        assertThatNoException().isThrownBy(() -> refreshTokens.rotate(winner.getFirst().token()));
    }

    @Test
    void rotateRefusesToRunInsideACallersTransaction() {
        var first = refreshTokens.startSession(UserId.random());
        var tx = new TransactionTemplate(txManager);

        // reuse revocation must survive the reuse exception, which needs rotate() to own its
        // transaction boundaries — an ambient transaction silently breaks that (ADR-0004)
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() ->
                tx.executeWithoutResult(status -> refreshTokens.rotate(first.token())));
    }

    private static Object valueOf(Future<Object> future) {
        try {
            return future.get();
        } catch (Exception e) {
            return e;
        }
    }

    @Test
    void anExpiredTokenIsRejected() {
        var first = refreshTokens.startSession(UserId.random());

        time.advance(TTL.plusDays(1));

        assertThatExceptionOfType(InvalidRefreshTokenException.class)
                .isThrownBy(() -> refreshTokens.rotate(first.token()));
    }
}
