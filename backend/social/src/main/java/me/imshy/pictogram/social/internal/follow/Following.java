package me.imshy.pictogram.social.internal.follow;

import java.time.Clock;
import java.time.Instant;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.social.UserFollowed;
import me.imshy.pictogram.social.UserUnfollowed;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class Following {

    private final Follows follows;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    Following(Follows follows, ApplicationEventPublisher events, Clock clock) {
        this.follows = follows;
        this.events = events;
        this.clock = clock;
    }

    public void follow(ViewerId viewer, UserId followed) {
        if (follows.existsByFollowerIdAndFollowedId(viewer.value(), followed.value())) {
            return;
        }

        Instant followedAt = clock.instant();
        try {
            follows.save(Follow.of(viewer.asUserId(), followed, followedAt));
        } catch (DataIntegrityViolationException alreadyFollowing) {
            return;
        }
        events.publishEvent(new UserFollowed(viewer.asUserId(), followed, followedAt));
    }

    public void unfollow(ViewerId viewer, UserId followed) {
        if (follows.deleteByFollowerIdAndFollowedId(viewer.value(), followed.value()) == 0) {
            return;
        }
        events.publishEvent(new UserUnfollowed(viewer.asUserId(), followed, clock.instant()));
    }
}
