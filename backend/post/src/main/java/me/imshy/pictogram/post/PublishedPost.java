package me.imshy.pictogram.post;

import java.time.Instant;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;

public record PublishedPost(PostId postId, UserId author, MediaId mediaId, String caption, Instant publishedAt) {}
