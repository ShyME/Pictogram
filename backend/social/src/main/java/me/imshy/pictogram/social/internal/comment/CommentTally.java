package me.imshy.pictogram.social.internal.comment;

import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.social.CommentCounts;
import org.springframework.stereotype.Service;

@Service
public class CommentTally implements CommentCounts {

    private final Comments comments;

    CommentTally(Comments comments) {
        this.comments = comments;
    }

    @Override
    public List<PostComments> of(Collection<PostId> posts) {
        List<UUID> ids = posts.stream().map(PostId::value).distinct().toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<UUID, Long> counts =
                comments.countsFor(ids).stream().collect(toMap(CommentCount::postId, CommentCount::count));
        return ids.stream()
                .map(id -> new PostComments(new PostId(id), counts.getOrDefault(id, 0L)))
                .toList();
    }
}
