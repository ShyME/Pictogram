package me.imshy.pictogram.identity;

import java.time.Instant;
import me.imshy.pictogram.shared.UserId;

/**
 * A person authenticated with Pictogram for the first time and now has a {@link UserId}.
 * {@code profile} will consume this later to offer onboarding; in v1 nothing does — it is
 * identity's forward contract (ADR-0002, CONTEXT-MAP).
 */
public record UserRegistered(UserId userId, String email, Instant registeredAt) {
}
