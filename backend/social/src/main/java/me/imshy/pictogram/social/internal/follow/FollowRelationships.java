package me.imshy.pictogram.social.internal.follow;

import static java.util.stream.Collectors.toMap;

import java.util.*;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.ViewerId;
import org.springframework.stereotype.Service;

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

        return ids.stream().map(id -> new Relationship(new UserId(id), followers.getOrDefault(id, 0L),
            following.getOrDefault(id, 0L), followed.contains(id))).toList();
    }

    private static Map<UUID, Long> tally(List<FollowCount> counts) {
        return counts.stream().collect(toMap(FollowCount::userId, FollowCount::count));
    }

    public record Relationship(UserId user, long followerCount, long followingCount, boolean followedByViewer) {
    }
}
