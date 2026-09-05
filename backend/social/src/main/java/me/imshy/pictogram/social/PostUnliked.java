package me.imshy.pictogram.social;

import java.time.Instant;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;

public record PostUnliked(PostId postId, ViewerId viewer, Instant unlikedAt) {
}
