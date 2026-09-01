package me.imshy.pictogram.identity.internal;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import me.imshy.pictogram.identity.UserRegistered;
import me.imshy.pictogram.identity.internal.accesstoken.AccessTokens;
import me.imshy.pictogram.identity.internal.refreshtoken.RefreshTokenService;
import me.imshy.pictogram.shared.UserId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The identity application service. Turns a verified {@link ExternalAccount} into a
 * {@link Session}, creating the {@code User} and emitting {@link UserRegistered} the first
 * time a given {@code (provider, subject)} is seen, and resolving the same {@link UserId}
 * on every later sign-in. Also rotates and revokes sessions.
 */
@Service
public class IdentityAuthentication {

    private final AppUsers users;
    private final AccessTokens accessTokens;
    private final RefreshTokenService refreshTokens;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    IdentityAuthentication(AppUsers users, AccessTokens accessTokens, RefreshTokenService refreshTokens,
            ApplicationEventPublisher events, Clock clock) {
        this.users = users;
        this.accessTokens = accessTokens;
        this.refreshTokens = refreshTokens;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public Session authenticate(ExternalAccount account) {
        AppUser user = users.findByProviderAndSubject(account.provider(), account.subject())
                .orElseGet(() -> register(account));
        return session(refreshTokens.startSession(user.userId()));
    }

    private AppUser register(ExternalAccount account) {
        Instant now = clock.instant();
        boolean weRegisteredThem = users.insertIfAbsent(UUID.randomUUID(),
                account.provider(), account.subject(), account.email(), now) == 1;
        AppUser user = users.findByProviderAndSubject(account.provider(), account.subject())
                .orElseThrow(() -> new IllegalStateException("user vanished right after being registered"));
        if (weRegisteredThem) {
            events.publishEvent(new UserRegistered(user.userId(), user.email(), user.registeredAt()));
        }
        return user;
    }

    public Session refresh(String refreshToken) {
        return session(refreshTokens.rotate(refreshToken));
    }

    public void signOut(String refreshToken) {
        refreshTokens.revokeFamilyOf(refreshToken);
    }

    private Session session(RefreshTokenService.Issued refresh) {
        return new Session(
                accessTokens.issue(refresh.user()),
                clock.instant().plus(accessTokens.ttl()),
                refresh.token(),
                refresh.expiresAt());
    }
}
