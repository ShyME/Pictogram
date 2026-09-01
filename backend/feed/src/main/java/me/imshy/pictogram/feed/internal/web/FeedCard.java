package me.imshy.pictogram.feed.internal.web;

import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;

record FeedCard(PostId postId, UserId author) {
}
