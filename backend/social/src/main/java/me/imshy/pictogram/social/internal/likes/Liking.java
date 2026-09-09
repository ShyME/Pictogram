package me.imshy.pictogram.social.internal.likes;

import java.time.Clock;
import java.time.Instant;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.social.PostLiked;
import me.imshy.pictogram.social.PostUnliked;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class Liking {

    private final Likes likes;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final TransactionTemplate ownTransaction;

    Liking(Likes likes, ApplicationEventPublisher events, Clock clock, PlatformTransactionManager transactionManager) {
        this.likes = likes;
        this.events = events;
        this.clock = clock;
        // Own REQUIRES_NEW tx: the like and its PostLiked outbox row (ADR-0015)
        // commit as one unit. Why not @Transactional: backend/social/CONTEXT.md.
        this.ownTransaction = new TransactionTemplate(transactionManager);
        this.ownTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void like(ViewerId viewer, PostId post) {
        if (likes.existsByPostIdAndViewerId(post.value(), viewer.value())) {
            return;
        }

        Instant likedAt = clock.instant();
        try {
            ownTransaction.executeWithoutResult(status -> {
                likes.save(Like.of(viewer, post, likedAt));
                events.publishEvent(new PostLiked(post, viewer, likedAt));
            });
        } catch (DataIntegrityViolationException lostTheRace) {
            // A concurrent like won the race and published its own PostLiked.
        }
    }

    public void unlike(ViewerId viewer, PostId post) {
        Instant unlikedAt = clock.instant();
        if (likes.deleteByPostIdAndViewerId(post.value(), viewer.value()) == 0) {
            return;
        }
        events.publishEvent(new PostUnliked(post, viewer, unlikedAt));
    }
}
