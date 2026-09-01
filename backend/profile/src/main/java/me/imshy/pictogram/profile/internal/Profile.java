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
