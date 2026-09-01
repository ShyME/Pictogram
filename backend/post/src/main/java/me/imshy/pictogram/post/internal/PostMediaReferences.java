package me.imshy.pictogram.post.internal;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import me.imshy.pictogram.media.PostReferences;
import me.imshy.pictogram.shared.MediaId;
import org.springframework.stereotype.Component;

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
