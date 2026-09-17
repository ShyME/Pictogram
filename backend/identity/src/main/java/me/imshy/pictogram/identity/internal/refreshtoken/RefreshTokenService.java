package me.imshy.pictogram.identity.internal.refreshtoken;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import me.imshy.pictogram.identity.internal.InvalidRefreshTokenException;
import me.imshy.pictogram.identity.internal.RefreshTokenReuseException;
import me.imshy.pictogram.shared.UserId;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

public class RefreshTokenService {

    private final RefreshTokens tokens;
    private final Clock clock;
    private final Duration ttl;
    private final Duration rotationGrace;
    private final TransactionTemplate tx;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(
            RefreshTokens tokens,
            Clock clock,
            Duration ttl,
            Duration rotationGrace,
            PlatformTransactionManager txManager) {
        this.tokens = tokens;
        this.clock = clock;
        this.ttl = ttl;
        this.rotationGrace = rotationGrace;
        this.tx = new TransactionTemplate(txManager);
    }

    public record Issued(UserId user, String token, Instant expiresAt) {}

    public Issued startSession(UserId user) {
        return tx.execute(status -> issue(user, UUID.randomUUID()));
    }

    public Issued rotate(String presentedToken) {
        requireNoAmbientTransaction();
        String presentedHash = hash(presentedToken);
        Rotation result = tx.execute(status -> rotateWithin(presentedHash));
        return switch (result) {
            case Rotation.Ok ok -> ok.issued();
            case Rotation.BenignReplay ignored ->
                throw new InvalidRefreshTokenException("Refresh token already rotated by a concurrent refresh");
            case Rotation.Reuse reuse -> {
                revokeFamily(reuse.familyId());
                throw new RefreshTokenReuseException(reuse.familyId());
            }
        };
    }

    private Rotation rotateWithin(String presentedHash) {
        RefreshToken row = tokens.findByTokenHash(presentedHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Unknown refresh token"));
        Instant now = clock.instant();
        if (row.isSpent()) {
            return outcomeFor(row.spentState(), row.familyId(), now);
        }
        if (row.isExpiredAt(now)) {
            throw new InvalidRefreshTokenException("Refresh token expired");
        }
        if (tokens.consumeIfLive(row.getId(), now) == 0) {
            SpentState spent = tokens.spentStateById(row.getId())
                    .orElseThrow(() -> new InvalidRefreshTokenException("Unknown refresh token"));
            return outcomeFor(spent, row.familyId(), now);
        }
        return new Rotation.Ok(issue(new UserId(row.userId()), row.familyId()));
    }

    private Rotation outcomeFor(SpentState spent, UUID familyId, Instant now) {
        return spent.isBenignRaceWithin(rotationGrace, now)
                ? new Rotation.BenignReplay()
                : new Rotation.Reuse(familyId);
    }

    public void revokeFamilyOf(String presentedToken) {
        requireNoAmbientTransaction();
        String presentedHash = hash(presentedToken);
        tx.executeWithoutResult(status -> tokens.findByTokenHash(presentedHash)
                .ifPresent(row -> tokens.revokeFamily(row.familyId(), clock.instant())));
    }

    private void revokeFamily(UUID familyId) {
        tx.executeWithoutResult(status -> tokens.revokeFamily(familyId, clock.instant()));
    }

    private Issued issue(UserId user, UUID familyId) {
        byte[] raw = new byte[32];
        random.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        Instant now = clock.instant();
        Instant expiresAt = now.plus(ttl);
        tokens.save(new RefreshToken(familyId, user.value(), hash(token), now, expiresAt));
        return new Issued(user, token, expiresAt);
    }

    private static void requireNoAmbientTransaction() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("RefreshTokenService drives its own transaction boundaries and "
                    + "must not run inside one — reuse revocation has to survive the reuse exception (ADR-0004)");
        }
    }

    private static String hash(String token) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha256.digest(token.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the platform", e);
        }
    }

    private sealed interface Rotation {
        record Ok(Issued issued) implements Rotation {}

        record BenignReplay() implements Rotation {}

        record Reuse(UUID familyId) implements Rotation {}
    }
}
