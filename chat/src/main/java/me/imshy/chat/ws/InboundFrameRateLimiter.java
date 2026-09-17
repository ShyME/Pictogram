package me.imshy.chat.ws;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

// A token bucket bounding how fast one connection may send inbound frames (#249). #192
// already bounds frame *size* (ChatWebSocketHandler.MAX_INBOUND_FRAME_PAYLOAD_LENGTH); nothing
// bounded frame *frequency* — a connected client could otherwise send as fast as the transport
// allows. One instance per connection (built alongside its ChatWebSocketHandler), so there is
// no concurrent access to guard against: WebSocketSession.receive() delivers inbound frames to
// a single subscriber, one at a time.
//
// Deliberately scoped to one connection's lifetime, not one caller's overall rate: a bucket
// resets on reconnect. Throttling reconnect/handshake frequency itself is a separate edge
// concern (#255), same as #192's frame-size bound never tried to police handshake volume.
class InboundFrameRateLimiter {

    // Sustained rate and burst headroom both comfortably above a human's fastest
    // realistic
    // typing/paste-and-send rate (a few messages/sec) — InboundFrameRateLimiterTest
    // pins both.
    static final int CAPACITY = 20;
    static final Duration REFILL_PERIOD = Duration.ofMillis(100); // 10 tokens/sec

    private final Clock clock;
    private double tokens;
    private Instant lastRefill;

    InboundFrameRateLimiter(Clock clock) {
        this.clock = clock;
        this.tokens = CAPACITY;
        this.lastRefill = clock.instant();
    }

    boolean tryConsume() {
        refill();
        if (tokens < 1)
            return false;
        tokens -= 1;
        return true;
    }

    private void refill() {
        Instant now = clock.instant();
        Duration elapsed = Duration.between(lastRefill, now);
        if (!elapsed.isPositive())
            return;
        double refilled = (double) elapsed.toNanos() / REFILL_PERIOD.toNanos();
        tokens = Math.min(CAPACITY, tokens + refilled);
        lastRefill = now;
    }
}
