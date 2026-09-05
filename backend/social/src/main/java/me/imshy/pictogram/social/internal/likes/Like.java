package me.imshy.pictogram.social.internal.likes;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;
import org.springframework.data.domain.Persistable;

@Entity
@Table(schema = "likes", name = "post_like")
@IdClass(LikeId.class)
class Like implements Persistable<LikeId> {

    @Id
    @Column(name = "post_id")
    private UUID postId;

    @Id
    @Column(name = "viewer_id")
    private UUID viewerId;

    @Column(name = "liked_at")
    private Instant likedAt;

    @Transient
    private boolean persisted;

    protected Like() {
    }

    private Like(UUID postId, UUID viewerId, Instant likedAt) {
        this.postId = postId;
        this.viewerId = viewerId;
        this.likedAt = likedAt;
    }

    static Like of(ViewerId viewer, PostId post, Instant likedAt) {
        return new Like(post.value(), viewer.value(), likedAt);
    }

    @Override
    public LikeId getId() {
        return new LikeId(postId, viewerId);
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
