package me.imshy.pictogram.identity.internal.refreshtoken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface RefreshTokens extends CrudRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshToken t set t.consumedAt = :when "
        + "where t.id = :id and t.consumedAt is null and t.revokedAt is null")
    int consumeIfLive(@Param("id") UUID id, @Param("when") Instant when);

    @Query("select new me.imshy.pictogram.identity.internal.refreshtoken.SpentState(t.consumedAt, t.revokedAt) "
        + "from RefreshToken t where t.id = :id")
    Optional<SpentState> spentStateById(@Param("id") UUID id);

    @Transactional
    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :when where t.familyId = :familyId and t.revokedAt is null")
    void revokeFamily(@Param("familyId") UUID familyId, @Param("when") Instant when);
}
