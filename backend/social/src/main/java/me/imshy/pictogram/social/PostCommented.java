package me.imshy.pictogram.social;

import java.time.Instant;
import java.util.UUID;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;
import org.springframework.modulith.events.Externalized;

@Externalized("pictogram.social")
public record PostCommented(PostId postId, UUID commentId, ViewerId viewer, Instant commentedAt) {
}
