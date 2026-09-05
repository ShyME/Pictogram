package me.imshy.pictogram.social.internal.likes;

import static java.util.stream.Collectors.toMap;

import java.util.*;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.social.LikeCounts;
import org.springframework.stereotype.Service;

@Service
public class LikeTally implements LikeCounts {

    private final Likes likes;

    LikeTally(Likes likes) {
        this.likes = likes;
    }

    @Override
    public List<PostLikes> of(ViewerId viewer, Collection<PostId> posts) {
        List<UUID> ids = distinctIds(posts);
        if (ids.isEmpty()) {
            return List.of();
        }
        return tally(ids, Set.copyOf(likes.likedByViewerAmong(viewer.value(), ids)));
    }

    @Override
    public List<PostLikes> of(Collection<PostId> posts) {
        List<UUID> ids = distinctIds(posts);
        if (ids.isEmpty()) {
            return List.of();
        }
        return tally(ids, Set.of());
    }

    private List<PostLikes> tally(List<UUID> ids, Set<UUID> likedByViewer) {
        Map<UUID, Long> counts = likes.countsFor(ids).stream().collect(toMap(LikeCount::postId, LikeCount::count));
        return ids.stream()
            .map(id -> new PostLikes(new PostId(id), counts.getOrDefault(id, 0L), likedByViewer.contains(id))).toList();
    }

    private static List<UUID> distinctIds(Collection<PostId> posts) {
        return posts.stream().map(PostId::value).distinct().toList();
    }
}
