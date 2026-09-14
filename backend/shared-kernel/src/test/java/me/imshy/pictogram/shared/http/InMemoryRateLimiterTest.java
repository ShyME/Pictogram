package me.imshy.pictogram.shared.http;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import me.imshy.pictogram.shared.UserId;
import org.junit.jupiter.api.Test;

class InMemoryRateLimiterTest {

    private final AdvanceableClock clock = new AdvanceableClock(Instant.parse("2026-09-11T09:00:00Z"));

    @Test
    void permitsCallsUpToTheConfiguredCapacity() {
        RateLimiter limiter = new InMemoryRateLimiter(clock, new RateLimitProperties(2, Duration.ofMinutes(1)));
        UserId user = UserId.random();

        assertThatNoException().isThrownBy(() -> limiter.requirePermit(user, "post-publish"));
        assertThatNoException().isThrownBy(() -> limiter.requirePermit(user, "post-publish"));
    }

    @Test
    void rejectsTheCallThatExceedsTheConfiguredCapacity() {
        RateLimiter limiter = new InMemoryRateLimiter(clock, new RateLimitProperties(2, Duration.ofMinutes(1)));
        UserId user = UserId.random();
        limiter.requirePermit(user, "post-publish");
        limiter.requirePermit(user, "post-publish");

        assertThatExceptionOfType(RateLimitExceededException.class)
            .isThrownBy(() -> limiter.requirePermit(user, "post-publish"));
    }

    @Test
    void tracksSeparateBucketsPerActionForTheSameUser() {
        RateLimiter limiter = new InMemoryRateLimiter(clock, new RateLimitProperties(1, Duration.ofMinutes(1)));
        UserId user = UserId.random();
        limiter.requirePermit(user, "post-publish");

        assertThatNoException().isThrownBy(() -> limiter.requirePermit(user, "follow"));
    }

    @Test
    void tracksSeparateBucketsPerUserForTheSameAction() {
        RateLimiter limiter = new InMemoryRateLimiter(clock, new RateLimitProperties(1, Duration.ofMinutes(1)));
        limiter.requirePermit(UserId.random(), "like");

        assertThatNoException().isThrownBy(() -> limiter.requirePermit(UserId.random(), "like"));
    }

    @Test
    void refillsOncePastWindowElapses() {
        RateLimiter limiter = new InMemoryRateLimiter(clock, new RateLimitProperties(1, Duration.ofMinutes(1)));
        UserId user = UserId.random();
        limiter.requirePermit(user, "comment");
        assertThatExceptionOfType(RateLimitExceededException.class)
            .isThrownBy(() -> limiter.requirePermit(user, "comment"));

        clock.advance(Duration.ofMinutes(1));

        assertThatNoException().isThrownBy(() -> limiter.requirePermit(user, "comment"));
    }

    private static final class AdvanceableClock extends Clock {

        private Instant now;

        AdvanceableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration by) {
            now = now.plus(by);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
