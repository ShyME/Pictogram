package me.imshy.pictogram.identity.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import me.imshy.pictogram.shared.UserId;

@Entity
@Table(schema = "identity", name = "app_user")
class AppUser {

    @Id
    private UUID id;
    private String provider;
    private String subject;
    private String email;
    private Instant registeredAt;

    protected AppUser() {
    }

    UserId userId() {
        return new UserId(id);
    }

    String email() {
        return email;
    }

    Instant registeredAt() {
        return registeredAt;
    }
}
