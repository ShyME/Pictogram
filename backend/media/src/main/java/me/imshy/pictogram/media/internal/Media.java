package me.imshy.pictogram.media.internal;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.UserId;
import org.springframework.data.domain.Persistable;

@Entity
@Table(schema = "media", name = "media")
class Media implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "size_bytes")
    private long sizeBytes;

    @Transient
    private boolean persisted;

    protected Media() {}

    private Media(UUID id, UUID ownerId, Instant createdAt, long sizeBytes) {
        this.id = id;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.sizeBytes = sizeBytes;
    }

    static Media uploadedBy(UserId owner, Instant at, long sizeBytes) {
        return new Media(UUID.randomUUID(), owner.value(), at, sizeBytes);
    }

    MediaId mediaId() {
        return new MediaId(id);
    }

    UserId ownerId() {
        return new UserId(ownerId);
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
