package me.imshy.pictogram.follow.internal;

import java.util.List;
import java.util.function.Function;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.http.Cursor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

@Service
public class FollowList {

    static final int DEFAULT_LIMIT = 24;
    static final int MAX_LIMIT = 48;

    private final Follows follows;

    FollowList(Follows follows) {
        this.follows = follows;
    }

    public Page followersOf(UserId user, Cursor after, Integer limit) {
        return page(limit, Follow::follower, fetch -> after == null
                ? follows.followersNewest(user.value(), fetch)
                : follows.followersBefore(user.value(), after.at(), after.id(), fetch));
    }

    public Page followingOf(UserId user, Cursor after, Integer limit) {
        return page(limit, Follow::followed, fetch -> after == null
                ? follows.followingNewest(user.value(), fetch)
                : follows.followingBefore(user.value(), after.at(), after.id(), fetch));
    }

    private Page page(Integer limit, Function<Follow, UserId> listedUser, Function<Limit, List<Follow>> rows) {
        int pageSize = clamp(limit);

        List<Follow> fetched = rows.apply(Limit.of(pageSize + 1));
        boolean hasMore = fetched.size() > pageSize;
        List<Follow> pageRows = hasMore ? fetched.subList(0, pageSize) : fetched;

        Cursor nextCursor = null;
        if (hasMore) {
            Follow last = pageRows.get(pageRows.size() - 1);
            nextCursor = new Cursor(last.followedAt(), last.getId());
        }

        return new Page(pageRows.stream().map(listedUser).toList(), nextCursor);
    }

    private static int clamp(Integer limit) {
        return limit == null ? DEFAULT_LIMIT : Math.clamp(limit, 1, MAX_LIMIT);
    }

    public record Page(List<UserId> items, Cursor nextCursor) {
    }
}
