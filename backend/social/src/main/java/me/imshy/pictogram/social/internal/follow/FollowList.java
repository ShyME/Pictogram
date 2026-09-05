package me.imshy.pictogram.social.internal.follow;

import java.util.List;
import java.util.function.Function;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.http.Cursor;
import me.imshy.pictogram.shared.http.KeysetWindow;
import me.imshy.pictogram.shared.http.Limits;
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
        return page(limit, Follow::follower,
            fetch -> after == null
                ? follows.followersNewest(user.value(), fetch)
                : follows.followersBefore(user.value(), after.at(), after.id(), fetch));
    }

    public Page followingOf(UserId user, Cursor after, Integer limit) {
        return page(limit, Follow::followed,
            fetch -> after == null
                ? follows.followingNewest(user.value(), fetch)
                : follows.followingBefore(user.value(), after.at(), after.id(), fetch));
    }

    private Page page(Integer limit, Function<Follow, UserId> listedUser, Function<Limit, List<Follow>> rows) {
        int pageSize = Limits.clamp(limit, DEFAULT_LIMIT, MAX_LIMIT);

        List<Follow> fetched = rows.apply(Limit.of(pageSize + 1));
        KeysetWindow<Follow> window = KeysetWindow.of(fetched, pageSize,
            last -> new Cursor(last.followedAt(), listedUser.apply(last).value()));

        return new Page(window.page().stream().map(listedUser).toList(), window.nextCursor());
    }

    public record Page(List<UserId> items, Cursor nextCursor) {
    }
}
