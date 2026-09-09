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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class Following {

    private final Follows follows;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final TransactionTemplate ownTransaction;

    Following(Follows follows, ApplicationEventPublisher events, Clock clock,
        PlatformTransactionManager transactionManager) {
        this.follows = follows;
        this.events = events;
        this.clock = clock;
        // Own REQUIRES_NEW tx: the follow and its UserFollowed outbox row (ADR-0015)
        // commit as one unit. Why not @Transactional: backend/social/CONTEXT.md.
        this.ownTransaction = new TransactionTemplate(transactionManager);
        this.ownTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void follow(ViewerId viewer, UserId followed) {
        if (follows.existsByFollowerIdAndFollowedId(viewer.value(), followed.value())) {
            return;
        }

        Instant followedAt = clock.instant();
        try {
            ownTransaction.executeWithoutResult(status -> {
                follows.save(Follow.of(viewer.asUserId(), followed, followedAt));
                events.publishEvent(new UserFollowed(viewer.asUserId(), followed, followedAt));
            });
        } catch (DataIntegrityViolationException lostTheRace) {
            // A concurrent follow won the race and published its own UserFollowed.
        }
    }

    public void unfollow(ViewerId viewer, UserId followed) {
        if (follows.deleteByFollowerIdAndFollowedId(viewer.value(), followed.value()) == 0) {
            return;
        }
        events.publishEvent(new UserUnfollowed(viewer.asUserId(), followed, clock.instant()));
    }
}
