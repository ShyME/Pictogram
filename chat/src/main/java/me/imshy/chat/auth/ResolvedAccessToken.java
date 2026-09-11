package me.imshy.chat.auth;

import java.time.Instant;
import me.imshy.chat.UserId;

public record ResolvedAccessToken(UserId userId, Instant expiresAt) {
}
