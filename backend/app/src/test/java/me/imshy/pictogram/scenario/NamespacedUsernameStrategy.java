package me.imshy.pictogram.scenario;

import me.imshy.pictogram.scenario.PictogramApp.Profile;

/**
 * {@link UsernameStrategy} for the container transport: its Postgres is never
 * truncated, so the scenarios' hard-coded usernames (e.g.
 * {@code "ada_lovelace"}) would collide across tests. Prefixes every username
 * on the way in and strips it from returned {@link Profile}s, so the scenario
 * body reads the handle it wrote and nothing leaks between tests (ADR-0007).
 */
final class NamespacedUsernameStrategy implements UsernameStrategy {

    private final String namespace;

    NamespacedUsernameStrategy(String namespace) {
        this.namespace = namespace;
    }

    /**
     * {@code Username}'s {@code ^[a-z0-9_]{3,20}$} check still applies with the
     * namespace prepended. A scenario literal longer than
     * {@code 20 - namespace.length()} otherwise fails only here, as a bare 400 from
     * {@code POST /api/profiles} in the container run — invisible to the in-process
     * transport, which needs no namespace.
     */
    @Override
    public String qualify(String username) {
        int budget = 20 - namespace.length();
        if (username.length() > budget) {
            throw new IllegalArgumentException(
                "Scenario username \"%s\" (%d chars) leaves no room for the %d-char blackbox namespace; keep scenario usernames to %d chars."
                    .formatted(username, username.length(), namespace.length(), budget));
        }
        return namespace + username;
    }

    @Override
    public Profile strip(Profile profile) {
        return profile.username().startsWith(namespace)
            ? new Profile(profile.userId(), profile.username().substring(namespace.length()), profile.displayName(),
                profile.bio())
            : profile;
    }
}
