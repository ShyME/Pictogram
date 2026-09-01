package me.imshy.pictogram.identity.internal;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

interface AppUsers extends CrudRepository<AppUser, UUID> {

    Optional<AppUser> findByProviderAndSubject(String provider, String subject);

    @Modifying
    @Query(value = """
            insert into identity.app_user (id, provider, subject, email, registered_at)
            values (:id, :provider, :subject, :email, :registeredAt)
            on conflict (provider, subject) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("provider") String provider,
            @Param("subject") String subject,
            @Param("email") String email,
            @Param("registeredAt") Instant registeredAt);
}
