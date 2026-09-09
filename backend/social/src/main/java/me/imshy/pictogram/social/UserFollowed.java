package me.imshy.pictogram.social;

import java.time.Instant;
import me.imshy.pictogram.shared.UserId;
import org.springframework.modulith.events.Externalized;

@Externalized("pictogram.social")
public record UserFollowed(UserId follower, UserId followed, Instant followedAt) {
}
