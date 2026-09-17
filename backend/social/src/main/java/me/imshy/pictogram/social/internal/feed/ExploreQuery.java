package me.imshy.pictogram.social.internal.feed;

import java.util.List;
import me.imshy.pictogram.shared.http.Cursor;

public interface ExploreQuery {

    Page pageFor(Cursor after, Integer limit);

    record Page(List<FeedPost> posts, Cursor nextCursor) {}
}
