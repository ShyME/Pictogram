package me.imshy.pictogram.profile.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import me.imshy.pictogram.shared.UserId;
import org.springframework.data.domain.Persistable;

/**
 * The public face of a user — {@link Username}, an optional display name and bio. Exactly
 * one per user once onboarded, created by {@link Onboarding} and keyed by {@link UserId}.
 * The user id is the only link back to identity (ADR-0002); there is no cross-schema key.
 *
 * <p>The primary key is assigned (the user's id), so {@link Persistable#isNew()} is tracked
 * explicitly: {@code save()} must {@code persist} a fresh row, never {@code merge}. A merge
 * would let a second onboarding that raced past the {@code existsById} guard silently
 * overwrite the existing profile instead of hitting the primary-key constraint.
 */
@Entity
@Table(schema = "profile", name = "profile")
class Profile implements Persistable<UUID> {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    private String username;

    @Column(name = "display_name")
    private String displayName;

    private String bio;

    @Column(name = "created_at")
    private Instant createdAt;

    @Transient
    private boolean persisted;

    protected Profile() {
    }

    private Profile(UserId userId, Username username, DisplayName displayName, Bio bio, Instant createdAt) {
        this.userId = userId.value();
        this.username = username.value();
        this.displayName = displayName.value();
        this.bio = bio.value();
        this.createdAt = createdAt;
    }

    static Profile onboard(UserId user, Username username, DisplayName displayName, Bio bio, Instant at) {
        return new Profile(user, username, displayName, bio, at);
    }

    /** Applies the new details and reports whether any field actually moved. */
    boolean edit(Username username, DisplayName displayName, Bio bio) {
        boolean changed = !this.username.equals(username.value())
                || !Objects.equals(this.displayName, displayName.value())
                || !Objects.equals(this.bio, bio.value());
        this.username = username.value();
        this.displayName = displayName.value();
        this.bio = bio.value();
        return changed;
    }

    @Override
    public UUID getId() {
        return userId;
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

    UserId userId() {
        return new UserId(userId);
    }

    String username() {
        return username;
    }

    String displayName() {
        return displayName;
    }

    String bio() {
        return bio;
    }
}
