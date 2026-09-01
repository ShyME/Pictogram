package me.imshy.pictogram.shared.http;

import java.util.Set;

/**
 * The cap on a {@code ?ids=} batch lookup. The client always knows exactly which ids it
 * needs — a page of a list it has already fetched — so a set larger than {@link #MAX} is a
 * caller bug or an attempt to turn one round trip into an unbounded query. Rejected as a
 * {@code 400} rather than silently truncated, so the caller notices.
 */
public final class BatchIds {

    /** Comfortably above a list page's largest size; a single screen never needs more. */
    public static final int MAX = 100;

    private BatchIds() {
    }

    public static <T> Set<T> checked(Set<T> ids) {
        if (ids.size() > MAX) {
            throw new OversizedBatchException(MAX, ids.size());
        }
        return ids;
    }
}
