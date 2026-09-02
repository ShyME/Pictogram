package me.imshy.pictogram.engagement;

import java.time.Instant;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;

public record PostLiked(PostId postId, ViewerId viewer, Instant likedAt) {}
