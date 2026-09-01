package me.imshy.pictogram.follow.internal;

import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.ViewerId;
import org.springframework.stereotype.Service;

/**
 * The batch relationship read for the follower / following list screens (#59): given the
 * page of user ids that screen is about to render, one call returns each user's follower and
 * following counts and whether the viewer follows them — the same shape a single
 * {@code GET /api/follows/{id}} returns, so the client can seed the per-user cache and the
 * reused follow buttons never fetch one at a time.
 *
 * <p>Three queries whatever the id-set size (never a loop of {@code isFollowing} per id).
 * Not on {@link me.imshy.pictogram.follow.FollowGraph}: only the web layer needs it, and
 * {@code feed} fans out over the flat {@code usersFollowedBy} list, not a batch.
 *
 * <p>{@code follow} owns no user table, so an id with no edges is not distinguishable from
 * one with no account — every requested id gets a record, zeros when it has no edges, rather
 * than being omitted the way the profile batch omits an unknown id.
 */
@Service
public class FollowRelationships {

    private final Follows follows;

    FollowRelationships(Follows follows) {
        this.follows = follows;
    }

    public List<Relationship> of(ViewerId viewer, Collection<UserId> users) {
        List<UUID> ids = users.stream().map(UserId::value).distinct().toList();
        if (ids.isEmpty()) {
            return List.of();
        }

        Map<UUID, Long> followers = tally(follows.followerCounts(ids));
        Map<UUID, Long> following = tally(follows.followingCounts(ids));
        Set<UUID> followed = Set.copyOf(follows.followedByViewerAmong(viewer.value(), ids));

        return ids.stream()
                .map(id -> new Relationship(new UserId(id),
                        followers.getOrDefault(id, 0L),
                        following.getOrDefault(id, 0L),
                        followed.contains(id)))
                .toList();
    }

    private static Map<UUID, Long> tally(List<FollowCount> counts) {
        return counts.stream().collect(toMap(FollowCount::userId, FollowCount::count));
    }

    /** One user's follow standing from the viewer's perspective — the batch item shape. */
    public record Relationship(UserId user, long followerCount, long followingCount, boolean followedByViewer) {
    }
}
