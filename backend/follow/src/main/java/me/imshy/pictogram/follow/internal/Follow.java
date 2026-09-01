package me.imshy.pictogram.follow.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.UUID;
import me.imshy.pictogram.shared.UserId;
import org.springframework.data.domain.Persistable;

/**
 * One directed edge of the follow graph: the follower follows the followed user. At most
 * one row per ordered pair (a unique constraint on the two columns); the surrogate
 * {@link #id} only exists so JPA has a single-column key. There is nothing to mutate — an
 * edge is created or deleted, never edited (follow/CONTEXT.md).
 *
 * <p>The key is assigned, so {@link Persistable#isNew()} is tracked explicitly and
 * {@code save()} inserts rather than merges.
 */
@Entity
@Table(schema = "follow", name = "follow")
class Follow implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "follower_id")
    private UUID followerId;

    @Column(name = "followed_id")
    private UUID followedId;

    @Transient
    private boolean persisted;

    protected Follow() {
    }

    private Follow(UUID id, UUID followerId, UUID followedId) {
        this.id = id;
        this.followerId = followerId;
        this.followedId = followedId;
    }

    static Follow of(UserId follower, UserId followed) {
        return new Follow(UUID.randomUUID(), follower.value(), followed.value());
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
