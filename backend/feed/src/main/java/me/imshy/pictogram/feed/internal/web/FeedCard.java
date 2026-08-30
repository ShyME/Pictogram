package me.imshy.pictogram.feed.internal.web;

import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;

/**
 * One entry in a feed page: a post by someone the viewer follows, referenced by id
 * (ADR-0002). The remaining card fields fill in when post publishing lands; today every
 * feed is empty, so this only fixes the wire shape a page returns.
 */
record FeedCard(PostId postId, UserId author) {
}
