package me.imshy.chat.ws;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class InboundFrameRateLimiterTest {

    @Test
    void aBurstUpToCapacityIsAccepted() {
        InboundFrameRateLimiter limiter = new InboundFrameRateLimiter(new FakeClock());

        for (int frame = 0; frame < InboundFrameRateLimiter.CAPACITY; frame++)
            assertThat(limiter.tryConsume())
                    .as("frame %d is within the burst capacity", frame)
                    .isTrue();
    }

    @Test
    void aFrameBeyondTheBurstCapacityIsRejected() {
        InboundFrameRateLimiter limiter = new InboundFrameRateLimiter(new FakeClock());
        for (int frame = 0; frame < InboundFrameRateLimiter.CAPACITY; frame++) limiter.tryConsume();

        assertThat(limiter.tryConsume()).isFalse();
    }

    @Test
    void tokensRefillOverTimeRatherThanStayingExhausted() {
        FakeClock clock = new FakeClock();
        InboundFrameRateLimiter limiter = new InboundFrameRateLimiter(clock);
        for (int frame = 0; frame < InboundFrameRateLimiter.CAPACITY; frame++) limiter.tryConsume();
        assertThat(limiter.tryConsume()).as("exhausted right after the burst").isFalse();

        clock.advance(InboundFrameRateLimiter.REFILL_PERIOD);

        assertThat(limiter.tryConsume())
                .as("one refill period grants exactly one more token")
                .isTrue();
        assertThat(limiter.tryConsume())
                .as("no further token until the next refill")
                .isFalse();
    }

    @Test
    void refillNeverExceedsTheBurstCapacity() {
        FakeClock clock = new FakeClock();
        InboundFrameRateLimiter limiter = new InboundFrameRateLimiter(clock);

        clock.advance(InboundFrameRateLimiter.REFILL_PERIOD.multipliedBy(InboundFrameRateLimiter.CAPACITY * 10L));

        for (int frame = 0; frame < InboundFrameRateLimiter.CAPACITY; frame++)
            assertThat(limiter.tryConsume())
                    .as("frame %d is within the burst capacity", frame)
                    .isTrue();
        assertThat(limiter.tryConsume())
                .as("idle time never banks more than the burst capacity")
                .isFalse();
    }

    private static final class FakeClock extends Clock {

        private Instant now = Instant.parse("2024-01-01T00:00:00Z");

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
