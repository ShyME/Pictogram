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
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Issues and rotates refresh tokens with reuse detection (ADR-0004). The raw token is a
 * 256-bit random value returned once; only its SHA-256 hash is stored. Rotation consumes
 * the presented row and issues the next in the same family.
 *
 * <p>Presenting a spent row is only treated as theft — the whole family is revoked — once it
 * has been spent longer than {@code rotationGrace}. Within that window a second presentation
 * of a just-consumed token is a benign concurrent refresh (a double-submit, a client retry,
 * React StrictMode's double effect): it is rejected with an {@link InvalidRefreshTokenException}
 * and the cookie is cleared, but the family stands, so the racer that won the rotation still
 * holds a usable cookie.
 *
 * <p>Transaction boundaries are explicit here rather than annotation-driven: reuse detection
 * must <em>persist</em> a family revocation and then <em>throw</em>. An exception rolls back
 * the transaction it is thrown from, so the revoke has to land in a separate transaction that
 * has already committed by the time the exception flies. Sequencing two {@link TransactionTemplate}
 * calls does that without the deadlock a {@code REQUIRES_NEW} revoke hit — the losing rotation's
 * conditional consume can hold an exclusive tuple lock until its transaction ends, and a second
 * connection revoking the family would block on it forever. This only holds with no ambient
 * transaction, so {@link #rotate} and {@link #revokeFamilyOf} — reached from
 * {@code IdentityAuthentication.refresh} / {@code signOut}, which are deliberately not
 * {@code @Transactional} — fail loud if one is active rather than trusting that. ({@link #startSession}
 * is exempt: it is the one entry point legitimately called inside {@code authenticate()}'s transaction.)
 */
class RefreshTokenService {

    private final RefreshTokens tokens;
    private final Clock clock;
    private final Duration ttl;
    private final Duration rotationGrace;
    private final TransactionTemplate tx;
    private final SecureRandom random = new SecureRandom();

    RefreshTokenService(RefreshTokens tokens, Clock clock, Duration ttl, Duration rotationGrace,
            PlatformTransactionManager txManager) {
        this.tokens = tokens;
        this.clock = clock;
        this.ttl = ttl;
        this.rotationGrace = rotationGrace;
        this.tx = new TransactionTemplate(txManager);
    }

    record Issued(UserId user, String token, Instant expiresAt) {
    }

    Issued startSession(UserId user) {
        return tx.execute(status -> issue(user, UUID.randomUUID()));
    }

    Issued rotate(String presentedToken) {
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
            // another rotation of this exact token beat us to it between the read and here;
            // re-read the spent state (not the stale row) to tell a benign race from theft
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

    void revokeFamilyOf(String presentedToken) {
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
        record Ok(Issued issued) implements Rotation {
        }

        /** The presented token was consumed within the grace window — a concurrent refresh, not theft. */
        record BenignReplay() implements Rotation {
        }

        record Reuse(UUID familyId) implements Rotation {
        }
    }
}
