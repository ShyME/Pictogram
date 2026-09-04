package me.imshy.pictogram.scenario;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import me.imshy.pictogram.scenario.PictogramApi.Actor;
import tools.jackson.databind.ObjectMapper;

/**
 * Blackbox transport for the {@code *Scenarios} (#20). Everything about speaking the API lives in
 * {@link HttpPictogramApi} (#51); this class only supplies what differs from the in-process run —
 * a base URI from the running stack and an {@link InteractiveLoginSignIn} — plus one thing the
 * in-process run gets for free: isolation.
 *
 * <p>The container's Postgres is never truncated, so the scenarios' hard-coded emails and usernames
 * ({@code "ada@example.com"}, {@code "ada_lovelace"}) would collide across methods. Each instance
 * (one per test — {@code @BeforeEach} rebuilds it) carries a {@link #namespace} token; it prefixes
 * every email and username on the way in and strips it from returned {@link PictogramApi.Profile}s,
 * so a scenario body reads the handle it wrote and nothing leaks between tests. This is the single
 * deviation from ADR-0007's "a config, not a second mapping layer".
 */
final class ContainerDriver implements PictogramApi {

    // :app:test runs one fork (Gradle default), so the per-JVM sequence alone makes the token
    // unique across a run; the random suffix only matters the day someone sets maxParallelForks > 1.
    private static final AtomicLong SEQUENCE = new AtomicLong();

    private final PictogramApi http;
    private final String namespace;

    ContainerDriver(URI baseUri, ObjectMapper json) {
        this.namespace = "bb%s%03d"
                .formatted(
                        Long.toString(SEQUENCE.incrementAndGet(), 36),
                        ThreadLocalRandom.current().nextInt(1000));
        this.http = new HttpPictogramApi(baseUri, json, new InteractiveLoginSignIn(baseUri));
    }

    @Override
    public Actor registerViaGoogle(String email) {
        return new NamespacedActor(http.registerViaGoogle(qualify(email)));
    }

    /** Prefix an identity (an email or a username) so it can't collide with another test's. */
    private String qualify(String identity) {
        return namespace + identity;
    }

    /**
     * {@link #qualify} for a username, which unlike an email has to survive {@code Username}'s
     * {@code ^[a-z0-9_]{3,20}$} check with the namespace prepended. A scenario literal longer than
     * {@code 20 - namespace.length()} otherwise fails only here, as a bare 400 from {@code POST
     * /api/profiles} in the blackbox run — invisible to the in-process transport, which needs no
     * namespace.
     */
    private String qualifyUsername(String username) {
        int budget = 20 - namespace.length();
        if (username.length() > budget) {
            throw new IllegalArgumentException(
                    "Scenario username \"%s\" (%d chars) leaves no room for the %d-char blackbox namespace; keep scenario usernames to %d chars."
                            .formatted(username, username.length(), namespace.length(), budget));
        }
        return qualify(username);
    }

    private Profile strip(Profile profile) {
        return profile.username().startsWith(namespace)
                ? new Profile(
                        profile.userId(),
                        profile.username().substring(namespace.length()),
                        profile.displayName(),
                        profile.bio())
                : profile;
    }

    /** Delegates every call, rewriting only the username-carrying ones so the scenario keeps its literals. */
    private final class NamespacedActor implements Actor {

        private final Actor delegate;

        private NamespacedActor(Actor delegate) {
            this.delegate = delegate;
        }

        @Override
        public Optional<Profile> currentProfile() {
            return delegate.currentProfile().map(ContainerDriver.this::strip);
        }

        @Override
        public Profile completeOnboarding(String username) {
            return strip(delegate.completeOnboarding(qualifyUsername(username)));
        }

        @Override
        public Profile completeOnboarding(String username, String displayName, String bio) {
            return strip(delegate.completeOnboarding(qualifyUsername(username), displayName, bio));
        }

        @Override
        public Profile editProfile(String username, String displayName, String bio) {
            return strip(delegate.editProfile(qualifyUsername(username), displayName, bio));
        }

        @Override
        public Optional<Profile> viewProfile(String username) {
            return delegate.viewProfile(qualifyUsername(username)).map(ContainerDriver.this::strip);
        }

        @Override
        public String uploadPhoto(byte[] image) {
            return delegate.uploadPhoto(image);
        }

        @Override
        public Post publishPost(String mediaId, String caption) {
            return delegate.publishPost(mediaId, caption);
        }

        @Override
        public DeleteOutcome deletePost(String postId) {
            return delegate.deletePost(postId);
        }

        @Override
        public List<Post> postsOf(String userId) {
            return delegate.postsOf(userId);
        }

        @Override
        public FollowOutcome follow(String userId) {
            return delegate.follow(userId);
        }

        @Override
        public void unfollow(String userId) {
            delegate.unfollow(userId);
        }

        @Override
        public FollowRelationship followRelationship(String userId) {
            return delegate.followRelationship(userId);
        }

        @Override
        public Map<String, FollowRelationship> followRelationships(String... userIds) {
            return delegate.followRelationships(userIds);
        }

        @Override
        public void like(String postId) {
            delegate.like(postId);
        }

        @Override
        public void unlike(String postId) {
            delegate.unlike(postId);
        }

        @Override
        public Map<String, PostLikes> likesOf(String... postIds) {
            return delegate.likesOf(postIds);
        }

        @Override
        public Comment comment(String postId, String body) {
            return delegate.comment(postId, body);
        }

        @Override
        public CommentPage commentsOn(String postId, String cursor, Integer limit) {
            return delegate.commentsOn(postId, cursor, limit);
        }

        @Override
        public AccountPage followers(String userId, String cursor, Integer limit) {
            return delegate.followers(userId, cursor, limit);
        }

        @Override
        public AccountPage following(String userId, String cursor, Integer limit) {
            return delegate.following(userId, cursor, limit);
        }

        @Override
        public FeedPage openFeed() {
            return delegate.openFeed();
        }

        @Override
        public FeedPage openFeed(String cursor, Integer limit) {
            return delegate.openFeed(cursor, limit);
        }
    }
}
