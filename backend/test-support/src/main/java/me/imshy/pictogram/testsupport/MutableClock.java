package me.imshy.pictogram.testsupport;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * A {@link Clock} a test winds forward by hand, so assertions about grace periods and token
 * lifetimes need no real waiting (ADR-0007). Used directly where the code under test takes a
 * {@code Clock}, or as the delegate of a {@code @MockitoBean Clock} where the real bean has
 * to be overridden.
 */
public final class MutableClock extends Clock {

    private Instant now;

    public MutableClock(Instant start) {
        this.now = start;
    }

    public static MutableClock at(String instant) {
        return new MutableClock(Instant.parse(instant));
    }

    public void advance(Duration by) {
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
