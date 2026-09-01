package me.imshy.pictogram.follow;

import java.time.Instant;
import me.imshy.pictogram.shared.UserId;

public record UserUnfollowed(UserId follower, UserId followed, Instant unfollowedAt) {}
