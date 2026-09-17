package me.imshy.pictogram.shared.http;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import me.imshy.pictogram.shared.UserId;
import org.springframework.stereotype.Component;

/**
 * A fixed-window counter per (user, action). Adequate for a single-instance
 * deployment (ADR-0011); a multi-instance one would need a shared store
 * instead.
 */
@Component
class InMemoryRateLimiter implements RateLimiter {

    // A key whose window has expired is only replaced, not removed, on its next use
    // — a key
    // that simply stops being used (an inactive or deleted account) would otherwise
    // sit in the
    // map forever. Sweeping every Nth call bounds that growth without a dedicated
    // scheduled job.
    private static final long SWEEP_EVERY = 10_000;

    private final Clock clock;
    private final RateLimitProperties properties;
    private final ConcurrentHashMap<Key, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong callCount = new AtomicLong();

    InMemoryRateLimiter(Clock clock, RateLimitProperties properties) {
        this.clock = clock;
        this.properties = properties;
    }

    @Override
    public void requirePermit(UserId userId, String action) {
        Instant now = clock.instant();
        if (callCount.incrementAndGet() % SWEEP_EVERY == 0) {
            windows.entrySet().removeIf(entry -> expired(entry.getValue(), now));
        }
        windows.compute(new Key(userId, action), (key, existing) -> {
            Window window = expired(existing, now)
                ? new Window(now.plus(properties.window()), new AtomicInteger(0))
                : existing;
            if (window.count().incrementAndGet() > properties.capacity()) {
                throw new RateLimitExceededException(action);
            }
            return window;
        });
    }

    private static boolean expired(Window window, Instant now) {
        return window == null || !now.isBefore(window.expiresAt());
    }

    private record Key(UserId userId, String action) {
    }

    private record Window(Instant expiresAt, AtomicInteger count) {
    }
}
