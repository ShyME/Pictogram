package me.imshy.pictogram.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import me.imshy.pictogram.shared.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RefreshTokenServiceTest extends ClockControlledModuleTest {

    /** Matches {@code pictogram.auth.refresh-token-ttl}'s default. */
    private static final Duration TTL = Duration.ofDays(30);

    @Autowired
    RefreshTokenService refreshTokens;

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
    void replayingASpentTokenRevokesTheWholeFamily() {
        var first = refreshTokens.startSession(UserId.random());
        var second = refreshTokens.rotate(first.token());

        assertThatExceptionOfType(RefreshTokenReuseException.class)
                .isThrownBy(() -> refreshTokens.rotate(first.token()));

        // the still-"current" token is dead too — the chain is gone, back to sign-in
        assertThatExceptionOfType(InvalidRefreshTokenException.class)
                .isThrownBy(() -> refreshTokens.rotate(second.token()));
    }

    @Test
    void twoRefreshesRacingOnOneTokenNeverBothSucceed() throws Exception {
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
        assertThat(outcomes).filteredOn(RefreshTokenService.Issued.class::isInstance).hasSize(1);
        assertThat(outcomes).anySatisfy(outcome ->
                assertThat(outcome).isInstanceOf(InvalidRefreshTokenException.class));
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
