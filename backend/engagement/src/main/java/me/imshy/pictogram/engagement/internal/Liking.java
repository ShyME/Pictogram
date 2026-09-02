package me.imshy.pictogram.engagement.internal;

import java.time.Clock;
import java.time.Instant;
import me.imshy.pictogram.engagement.PostLiked;
import me.imshy.pictogram.engagement.PostUnliked;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class Liking {

    private final Likes likes;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    Liking(Likes likes, ApplicationEventPublisher events, Clock clock) {
        this.likes = likes;
        this.events = events;
        this.clock = clock;
    }

    public void like(ViewerId viewer, PostId post) {
        if (likes.existsByPostIdAndViewerId(post.value(), viewer.value())) {
            return;
        }

        Instant likedAt = clock.instant();
        try {
            likes.save(Like.of(viewer, post, likedAt));
        } catch (DataIntegrityViolationException alreadyLiked) {
            return;
        }
        events.publishEvent(new PostLiked(post, viewer, likedAt));
    }

    public void unlike(ViewerId viewer, PostId post) {
        if (likes.deleteByPostIdAndViewerId(post.value(), viewer.value()) == 0) {
            return;
        }
        events.publishEvent(new PostUnliked(post, viewer, clock.instant()));
    }
}
