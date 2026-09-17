package me.imshy.pictogram.social.internal.feed;

final class FeedPageSize {

    static final int DEFAULT = 12;
    static final int MAX = 30;

    static int clamp(Integer limit) {
        return limit == null ? DEFAULT : Math.clamp(limit, 1, MAX);
    }

    private FeedPageSize() {}
}
