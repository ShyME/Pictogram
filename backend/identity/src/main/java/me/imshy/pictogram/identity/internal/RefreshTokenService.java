package me.imshy.pictogram.identity.internal;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import me.imshy.pictogram.shared.UserId;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Issues and rotates refresh tokens with reuse detection (ADR-0004). The raw token is a
 * 256-bit random value returned once; only its SHA-256 hash is stored. Rotation consumes
 * the presented row and issues the next in the same family; presenting a spent row revokes
 * the family.
 *
 * <p>Transaction boundaries are explicit here rather than annotation-driven: reuse detection
 * must <em>persist</em> a family revocation and then <em>throw</em>. An exception rolls back
 * the transaction it is thrown from, so the revoke has to land in a separate transaction that
 * has already committed by the time the exception flies. Sequencing two {@link TransactionTemplate}
 * calls does that without the deadlock a {@code REQUIRES_NEW} revoke hit — the losing rotation's
 * conditional consume can hold an exclusive tuple lock until its transaction ends, and a second
 * connection revoking the family would block on it forever.
 */
class RefreshTokenService {

    private final RefreshTokens tokens;
    private final Clock clock;
    private final Duration ttl;
    private final TransactionTemplate tx;
    private final SecureRandom random = new SecureRandom();

    RefreshTokenService(RefreshTokens tokens, Clock clock, Duration ttl, PlatformTransactionManager txManager) {
        this.tokens = tokens;
        this.clock = clock;
        this.ttl = ttl;
        this.tx = new TransactionTemplate(txManager);
    }

    record Issued(UserId user, String token, Instant expiresAt) {
    }

    Issued startSession(UserId user) {
        return tx.execute(status -> issue(user, UUID.randomUUID()));
    }

    Issued rotate(String presentedToken) {
        String presentedHash = hash(presentedToken);
        Rotation result = tx.execute(status -> rotateWithin(presentedHash));
        if (result instanceof Rotation.Reuse reuse) {
            revokeFamily(reuse.familyId());
            throw new RefreshTokenReuseException(reuse.familyId());
        }
        return ((Rotation.Ok) result).issued();
    }

    private Rotation rotateWithin(String presentedHash) {
        RefreshToken row = tokens.findByTokenHash(presentedHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Unknown refresh token"));
        Instant now = clock.instant();
        if (row.isSpent()) {
            return new Rotation.Reuse(row.familyId());
        }
        if (row.isExpiredAt(now)) {
            throw new InvalidRefreshTokenException("Refresh token expired");
        }
        if (tokens.consumeIfLive(row.getId(), now) == 0) {
            // another rotation of this exact token beat us to it between the read and here
            return new Rotation.Reuse(row.familyId());
        }
        return new Rotation.Ok(issue(new UserId(row.userId()), row.familyId()));
    }

    void revokeFamilyOf(String presentedToken) {
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

    private static String hash(String token) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha256.digest(token.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the platform", e);
        }
    }

    private sealed interface Rotation {
        record Ok(Issued issued) implements Rotation {
        }

        record Reuse(UUID familyId) implements Rotation {
        }
    }
}
