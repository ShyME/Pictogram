package me.imshy.pictogram.identity.internal;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

interface RefreshTokens extends CrudRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Marks the row consumed only if it is still live, and reports whether it did (1) or
     * another concurrent rotation got there first (0). This one statement is what makes
     * rotation race-safe — no double-spend, no lost reuse detection.
     */
    @Modifying
    @Query("update RefreshToken t set t.consumedAt = :when "
            + "where t.id = :id and t.consumedAt is null and t.revokedAt is null")
    int consumeIfLive(@Param("id") UUID id, @Param("when") Instant when);

    /**
     * Revokes every still-live token in the family. {@link RefreshTokenService} calls this from
     * its own {@code TransactionTemplate} once the rotation transaction has committed, so it runs
     * on a clean connection holding no locks and its write survives the reuse exception thrown
     * next.
     */
    @Transactional
    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :when where t.familyId = :familyId and t.revokedAt is null")
    void revokeFamily(@Param("familyId") UUID familyId, @Param("when") Instant when);
}
