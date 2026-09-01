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

    private static final Duration TTL = Duration.ofDays(30);

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
        assertThatNoException().isThrownBy(() -> refreshTokens.rotate(winner.getFirst().token()));
    }

    @Test
    void rotateRefusesToRunInsideACallersTransaction() {
        var first = refreshTokens.startSession(UserId.random());
        var tx = new TransactionTemplate(txManager);

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
