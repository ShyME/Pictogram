package me.imshy.pictogram.shared.http;

import java.util.List;
import java.util.function.Function;

/**
 * The keyset paging mechanic: given the {@code pageSize + 1} rows an over-fetch returned, trim to
 * {@code pageSize} and, when a further row exists, build the next {@link Cursor} from the last row the
 * client will see (keyset paging resumes <em>after</em> that row, not from the dropped one).
 */
public record KeysetWindow<T>(List<T> page, Cursor nextCursor) {

    public KeysetWindow {
        page = List.copyOf(page);
    }

    public static <T> KeysetWindow<T> of(List<T> overFetched, int pageSize, Function<? super T, Cursor> cursorOf) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be at least 1, was " + pageSize);
        }
        boolean hasMore = overFetched.size() > pageSize;
        List<T> page = hasMore ? overFetched.subList(0, pageSize) : overFetched;
        Cursor nextCursor = hasMore ? cursorOf.apply(page.get(page.size() - 1)) : null;
        return new KeysetWindow<>(page, nextCursor);
    }
}
