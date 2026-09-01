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

        /**
         * Edit the caller's own profile — new display name and bio, and a new username
         * (pass the current one to leave the handle unchanged). A rename frees the old
         * handle immediately.
         */
        Profile editProfile(String username, String displayName, String bio);

        /** Open someone's public profile by username — empty when no such user exists. */
        Optional<Profile> viewProfile(String username);

        /** Upload one photo; comes back as the {@code MediaId} a post is published against. */
        String uploadPhoto(byte[] image);

        /** Publish an uploaded photo with an optional caption — it becomes a {@link Post}. */
        Post publishPost(String mediaId, String caption);

        /**
         * Delete a post and report what the server did — {@link DeleteOutcome#DELETED} for
         * the author, {@link DeleteOutcome#FORBIDDEN} for anyone else — so a scenario can
         * assert on the outcome rather than a thrown error.
         */
        DeleteOutcome deletePost(String postId);

        /** A user's post grid, newest first — the first page. */
        List<Post> postsOf(String userId);

        /** The viewer's home feed. */
        FeedPage openFeed();
    }

    /** What the server did with a delete request. */
    enum DeleteOutcome {
        DELETED, FORBIDDEN
    }

    record Profile(String userId, String username, String displayName, String bio) {
    }

    record Post(String postId, String authorId, String mediaId, String caption, String publishedAt) {
    }

    record FeedPage(List<Object> items, String nextCursor) {

        public boolean isEmpty() {
            return items.isEmpty() && nextCursor == null;
        }
    }
}
