package me.imshy.pictogram.social;

import java.time.Instant;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;
import org.springframework.modulith.events.Externalized;

@Externalized("pictogram.social")
public record PostLiked(PostId postId, ViewerId viewer, Instant likedAt) {
}
