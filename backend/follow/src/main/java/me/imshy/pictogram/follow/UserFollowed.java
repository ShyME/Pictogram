package me.imshy.pictogram.follow;

import java.time.Instant;
import me.imshy.pictogram.shared.UserId;

public record UserFollowed(UserId follower, UserId followed, Instant followedAt) {}
