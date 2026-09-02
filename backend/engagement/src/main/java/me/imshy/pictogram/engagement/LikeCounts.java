package me.imshy.pictogram.engagement;

import java.util.Collection;
import java.util.List;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;

public interface LikeCounts {

    List<PostLikes> of(ViewerId viewer, Collection<PostId> posts);

    List<PostLikes> of(Collection<PostId> posts);

    record PostLikes(PostId post, long likeCount, boolean likedByViewer) {}
}
