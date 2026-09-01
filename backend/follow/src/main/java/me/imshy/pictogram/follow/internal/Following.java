package me.imshy.pictogram.follow.internal;

import java.time.Clock;
import me.imshy.pictogram.follow.UserFollowed;
import me.imshy.pictogram.follow.UserUnfollowed;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.ViewerId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * The follow and unfollow commands (follow/CONTEXT.md). Both are idempotent: following a
 * user already followed, or unfollowing one not followed, changes nothing and returns
 * quietly. A {@link UserFollowed} / {@link UserUnfollowed} is emitted only on a real state
 * change. Following yourself is rejected as a {@link SelfFollowException}.
 *
 * <p>Not {@code @Transactional}, like {@link me.imshy.pictogram.profile.internal.Onboarding}:
 * the write commits in its own transaction and the event publishes after it, so there is no
 * outer boundary for a listener to roll back.
 */
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
        if (viewer.value().equals(followed.value())) {
            throw new SelfFollowException();
        }
        if (follows.existsByFollowerIdAndFollowedId(viewer.value(), followed.value())) {
            return;
        }

        try {
            follows.save(Follow.of(viewer.asUserId(), followed));
        } catch (DataIntegrityViolationException alreadyFollowing) {
            // A concurrent follow of the same pair won the unique constraint — the edge
            // exists either way, so this call still changed nothing.
            return;
        }
        events.publishEvent(new UserFollowed(viewer.asUserId(), followed, clock.instant()));
    }

    public void unfollow(ViewerId viewer, UserId followed) {
        if (follows.deleteByFollowerIdAndFollowedId(viewer.value(), followed.value()) == 0) {
            return;
        }
        events.publishEvent(new UserUnfollowed(viewer.asUserId(), followed, clock.instant()));
    }
}
