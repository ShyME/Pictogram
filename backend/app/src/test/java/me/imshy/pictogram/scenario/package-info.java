/**
 * Acceptance journeys, one per user goal, run against two transports (ADR-0007).
 *
 * <p><b>The scenario.</b> Each journey is a {@code *Scenarios} interface of {@code @Test default}
 * methods written against {@link me.imshy.pictogram.scenario.PictogramApi} — intention-revealing
 * actions ({@code registerViaGoogle}, {@code completeOnboarding}, {@code publishPost}, {@code follow},
 * {@code openFeed}, {@code like}) plus reads for assertions. The body never learns which transport it
 * is on; it reaches the driver through {@link me.imshy.pictogram.scenario.PictogramScenario#pictogram()}.
 *
 * <p><b>The transports.</b> {@code InProcessScenarioTest} implements every mixin over
 * {@link me.imshy.pictogram.scenario.InProcessDriver} — a full {@code @SpringBootTest} with
 * Testcontainers, {@code @Tag("fast")}, every build. {@code BlackboxScenarioTest} implements the same
 * list over {@link me.imshy.pictogram.scenario.ContainerDriver} — the built image over HTTP,
 * {@code @Tag("blackbox")}, the CI {@code main} job.
 *
 * <p><b>Speak vs. transport (#51).</b> {@link me.imshy.pictogram.scenario.HttpPictogramApi} owns all
 * request building, JSON mapping, multipart encoding and outcome translation against a base URI;
 * {@link me.imshy.pictogram.scenario.SignIn} owns "how a session is minted". {@code InProcessDriver}
 * composes {@code HttpPictogramApi} with a random-port URI and a {@code mock-oauth2-server} callback;
 * {@code ContainerDriver} composes it with a compose URI and
 * {@link me.imshy.pictogram.scenario.InteractiveLoginSignIn} (the real redirect + form handshake),
 * and additionally namespaces identities so the un-truncated container DB stays isolated per test.
 */
package me.imshy.pictogram.scenario;
