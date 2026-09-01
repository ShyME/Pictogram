package me.imshy.pictogram.post.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;
import org.springframework.data.domain.Persistable;

/**
 * One published post: an {@link #author}, the {@link MediaId} of the image, an optional
 * {@link Caption}, and the instant it was published. Immutable after {@link #publish} —
 * there are no mutators; the only later change is the row being deleted (post/CONTEXT.md).
 *
 * <p>The primary key is assigned, so {@link Persistable#isNew()} is tracked explicitly:
 * {@code save()} must {@code persist} a fresh row, never {@code merge}.
 */
@Entity
@Table(schema = "post", name = "post")
class Post implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "author_id")
    private UUID authorId;

    @Column(name = "media_id")
    private UUID mediaId;

    private String caption;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Transient
    private boolean persisted;

    protected Post() {
    }

    private Post(UUID id, UUID authorId, UUID mediaId, String caption, Instant publishedAt) {
        this.id = id;
        this.authorId = authorId;
        this.mediaId = mediaId;
        this.caption = caption;
        this.publishedAt = publishedAt;
    }

    static Post publish(UserId author, MediaId mediaId, Caption caption, Instant at) {
        return new Post(UUID.randomUUID(), author.value(), mediaId.value(), caption.value(), at);
    }

    PostId postId() {
        return new PostId(id);
    }

    UserId author() {
        return new UserId(authorId);
    }

    MediaId mediaId() {
        return new MediaId(mediaId);
    }

    String caption() {
        return caption;
    }

    Instant publishedAt() {
        return publishedAt;
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
