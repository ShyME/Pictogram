package me.imshy.pictogram.profile;

import java.time.Instant;
import me.imshy.pictogram.shared.UserId;

public record ProfileUpdated(UserId userId, String username, Instant updatedAt) {
}
