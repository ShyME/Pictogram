package me.imshy.pictogram.social.internal.follow;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import me.imshy.pictogram.shared.UserId;
import org.springframework.data.domain.Persistable;

@Entity
@Table(schema = "follow", name = "follow")
@IdClass(FollowId.class)
class Follow implements Persistable<FollowId> {

    @Id
    @Column(name = "follower_id")
    private UUID followerId;

    @Id
    @Column(name = "followed_id")
    private UUID followedId;

    @Column(name = "followed_at")
    private Instant followedAt;

    @Transient
    private boolean persisted;

    protected Follow() {
    }

    private Follow(UUID followerId, UUID followedId, Instant followedAt) {
        this.followerId = followerId;
        this.followedId = followedId;
        this.followedAt = followedAt;
    }

    static Follow of(UserId follower, UserId followed, Instant followedAt) {
        return new Follow(follower.value(), followed.value(), followedAt);
    }

    UserId follower() {
        return new UserId(followerId);
    }

    UserId followed() {
        return new UserId(followedId);
    }

    Instant followedAt() {
        return followedAt;
    }

    @Override
    public FollowId getId() {
        return new FollowId(followerId, followedId);
    }

    @Override
    public boolean isNew() {
        return !persisted;
    }

    @PostPersist
    @PostLoad
    void markPersisted() {
        this.persisted = true;
    }
}
