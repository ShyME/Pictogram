package me.imshy.pictogram.identity;

import java.time.Instant;
import me.imshy.pictogram.shared.UserId;

public record UserRegistered(UserId userId, String email, Instant registeredAt) {}
