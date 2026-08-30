package me.imshy.pictogram.scenario;

import java.util.List;
import java.util.Optional;

/**
 * The scenario driver (ADR-0007): user-goal actions written once and run against a
 * transport. {@link InProcessDriver} is the in-process transport — {@code @SpringBootTest}
 * over HTTP with Testcontainers Postgres and {@code mock-oauth2-server}; a black-box
 * transport against the running container image comes later.
 *
 * <p>Actions read as intentions ({@code registerViaGoogle}, {@code completeOnboarding},
 * {@code openFeed}), never as HTTP mechanics, so a scenario body is a story.
 */
public interface PictogramApi {

    /** A person completes Google sign-in and comes away holding a Pictogram session. */
    Actor registerViaGoogle(String email);

    /** One signed-in person, acting through their session. */
    interface Actor {

        /** The caller's own profile, or empty while they are "not yet onboarded". */
        Optional<Profile> currentProfile();

        /** Choose a username — this creates the profile. */
        Profile completeOnboarding(String username);

        /** Choose a username plus an optional display name and bio. */
        Profile completeOnboarding(String username, String displayName, String bio);

        /** The viewer's home feed. */
        FeedPage openFeed();
    }

    record Profile(String userId, String username, String displayName, String bio) {
    }

    record FeedPage(List<Object> items, String nextCursor) {

        public boolean isEmpty() {
            return items.isEmpty() && nextCursor == null;
        }
    }
}
