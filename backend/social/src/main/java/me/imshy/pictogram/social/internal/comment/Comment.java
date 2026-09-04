package me.imshy.pictogram.social.internal.comment;

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
@Table(schema = "comment", name = "comment")
class Comment implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "post_id")
    private UUID postId;

    @Column(name = "viewer_id")
    private UUID viewerId;

    private String body;

    @Column(name = "created_at")
    private Instant createdAt;

    @Transient
    private boolean persisted;

    protected Comment() {}

    private Comment(UUID id, UUID postId, UUID viewerId, String body, Instant createdAt) {
        this.id = id;
        this.postId = postId;
        this.viewerId = viewerId;
        this.body = body;
        this.createdAt = createdAt;
    }

    static Comment write(ViewerId viewer, PostId post, CommentBody body, Instant createdAt) {
        return new Comment(UUID.randomUUID(), post.value(), viewer.value(), body.value(), createdAt);
    }

    Instant createdAt() {
        return createdAt;
    }

    PostComment view() {
        return new PostComment(id, new PostId(postId), new ViewerId(viewerId), body, createdAt);
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
