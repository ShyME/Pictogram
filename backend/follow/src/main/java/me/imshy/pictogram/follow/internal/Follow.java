package me.imshy.pictogram.follow.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;
import me.imshy.pictogram.shared.UserId;
import org.springframework.data.domain.Persistable;

@Entity
@Table(schema = "follow", name = "follow")
class Follow implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "follower_id")
    private UUID followerId;

    @Column(name = "followed_id")
    private UUID followedId;

    @Column(name = "followed_at")
    private Instant followedAt;

    @Transient
    private boolean persisted;

    protected Follow() {
    }

    private Follow(UUID id, UUID followerId, UUID followedId, Instant followedAt) {
        this.id = id;
        this.followerId = followerId;
        this.followedId = followedId;
        this.followedAt = followedAt;
    }

    static Follow of(UserId follower, UserId followed, Instant followedAt) {
        return new Follow(UUID.randomUUID(), follower.value(), followed.value(), followedAt);
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
    public UUID getId() {
        return id;
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
