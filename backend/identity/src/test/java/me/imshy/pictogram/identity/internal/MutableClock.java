package me.imshy.pictogram.identity.internal;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A {@link Clock} tests can wind forward, so token-lifetime assertions need no real waiting (ADR-0007). */
final class MutableClock extends Clock {

    private Instant now;

    MutableClock(Instant start) {
        this.now = start;
    }

    static MutableClock at(String instant) {
        return new MutableClock(Instant.parse(instant));
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
