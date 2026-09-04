package me.imshy.pictogram.social;

import java.util.Collection;
import java.util.List;
import me.imshy.pictogram.shared.PostId;

/**
 * How many comments each of a set of posts has, in one batch call — the read the feed card
 * and the profile grid use to show a comment count without a request per post (ADR-0005).
 * Mirrors {@link LikeCounts}: a post with no comments reads as {@code (id, 0)}, and the
 * batch never omits a requested id.
 */
public interface CommentCounts {

    List<PostComments> of(Collection<PostId> posts);

    record PostComments(PostId post, long commentCount) {}
}
