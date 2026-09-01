package me.imshy.pictogram.media.internal;

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

    @Transient
    private boolean persisted;

    protected Media() {}

    private Media(UUID id, UUID ownerId, Instant createdAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
    }

    static Media uploadedBy(UserId owner, Instant at) {
        return new Media(UUID.randomUUID(), owner.value(), at);
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
