package me.imshy.pictogram.post;

import java.time.Instant;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;

public record PostDeleted(PostId postId, UserId authorId, MediaId mediaId, Instant deletedAt) {
}
