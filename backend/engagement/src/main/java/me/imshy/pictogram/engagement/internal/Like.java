package me.imshy.pictogram.engagement.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;
import org.springframework.data.domain.Persistable;

@Entity
@Table(schema = "engagement", name = "post_like")
class Like implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "post_id")
    private UUID postId;

    @Column(name = "viewer_id")
    private UUID viewerId;

    @Column(name = "liked_at")
    private Instant likedAt;

    @Transient
    private boolean persisted;

    protected Like() {}

    private Like(UUID id, UUID postId, UUID viewerId, Instant likedAt) {
        this.id = id;
        this.postId = postId;
        this.viewerId = viewerId;
        this.likedAt = likedAt;
    }

    static Like of(ViewerId viewer, PostId post, Instant likedAt) {
        return new Like(UUID.randomUUID(), post.value(), viewer.value(), likedAt);
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
