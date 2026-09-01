package me.imshy.pictogram.post;

import java.util.Collection;
import java.util.List;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.http.Cursor;

public interface PublishedPosts {

    Page byAuthors(Collection<UserId> authors, Cursor after, int limit);

    record Page(List<PublishedPost> posts, Cursor nextCursor) {
    }
}
