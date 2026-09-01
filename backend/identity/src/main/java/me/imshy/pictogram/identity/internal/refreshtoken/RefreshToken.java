package me.imshy.pictogram.identity.internal.refreshtoken;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

@Entity
@Table(schema = "identity", name = "refresh_token")
class RefreshToken implements Persistable<UUID> {

    @Id
    private UUID id;

    private UUID familyId;
    private UUID userId;
    private String tokenHash;
    private Instant issuedAt;
    private Instant expiresAt;
    private Instant consumedAt;
    private Instant revokedAt;

    @Transient
    private boolean unsaved;

    protected RefreshToken() {}

    RefreshToken(UUID familyId, UUID userId, String tokenHash, Instant issuedAt, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.familyId = familyId;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.unsaved = true;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return unsaved;
    }

    @PostPersist
    @PostLoad
    void markSaved() {
        this.unsaved = false;
    }

    boolean isSpent() {
        return consumedAt != null || revokedAt != null;
    }

    SpentState spentState() {
        return new SpentState(consumedAt, revokedAt);
    }

    boolean isExpiredAt(Instant when) {
        return !when.isBefore(expiresAt);
    }

    UUID familyId() {
        return familyId;
    }

    UUID userId() {
        return userId;
    }
}
