package me.imshy.pictogram.post.internal;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import me.imshy.pictogram.media.PostReferences;
import me.imshy.pictogram.shared.MediaId;
import org.springframework.stereotype.Component;

/**
 * post's side of media's {@link PostReferences} port (CONTEXT-MAP: post → media). A media id
 * is "referenced" exactly while a {@code post} row still holds it; a deleted post is a hard
 * delete, so its media stops being reported the moment the row is gone (#16).
 */
@Component
class PostMediaReferences implements PostReferences {

    private final Posts posts;

    PostMediaReferences(Posts posts) {
        this.posts = posts;
    }

    @Override
    public Set<MediaId> referencedAmong(Collection<MediaId> candidates) {
        if (candidates.isEmpty()) {
            return Set.of();
        }
        Set<UUID> ids = candidates.stream().map(MediaId::value).collect(Collectors.toSet());
        return posts.mediaIdsAmong(ids).stream().map(MediaId::new).collect(Collectors.toSet());
    }
}
