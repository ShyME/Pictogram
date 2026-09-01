/**
 * The directed follow graph — who follows whom, at most one edge per ordered pair, self-follow
 * rejected. Follow and unfollow are idempotent. Emits {@code UserFollowed} / {@code UserUnfollowed};
 * answers follower / following counts and "is A following B" through {@code FollowGraph}.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Follow")
package me.imshy.pictogram.follow;
