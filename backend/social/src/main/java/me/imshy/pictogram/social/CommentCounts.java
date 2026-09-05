package me.imshy.pictogram.social;

import java.util.Collection;
import java.util.List;
import me.imshy.pictogram.shared.PostId;

public interface CommentCounts {

    List<PostComments> of(Collection<PostId> posts);

    record PostComments(PostId post, long commentCount) {
    }
}
