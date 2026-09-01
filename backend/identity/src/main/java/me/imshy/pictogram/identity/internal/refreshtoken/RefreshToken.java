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

/**
 * One issued refresh token, stored only as a SHA-256 hash. A {@code familyId} groups the
 * rotation chain started at sign-in: each rotation consumes its row and writes the next in
 * the same family. A row is "spent" once consumed or revoked; presenting a spent token more
 * than the rotation grace after it was consumed is reuse and revokes the whole family, while
 * a presentation within that grace is a benign concurrent refresh (ADR-0004).
 *
 * <p>Implements {@link Persistable} with an assigned id so {@code save()} does a plain
 * {@code INSERT} rather than a {@code SELECT}-then-{@code INSERT} (Spring Data JPA otherwise
 * treats an assigned id as a detached entity).
 */
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

    protected RefreshToken() {
    }

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
