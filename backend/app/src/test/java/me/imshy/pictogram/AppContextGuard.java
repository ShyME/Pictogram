package me.imshy.pictogram;

import me.imshy.pictogram.testsupport.DistinctContextGuard;

/**
 * Ceiling on the number of distinct {@code ApplicationContext}s the full-boot app suite may build.
 * Measured floor after the #78 collapse is 8:
 *
 * <ul>
 *   <li>MockMvc group ({@link AppIntegrationTest})
 *   <li>RANDOM_PORT no-OAuth group ({@link AppWebIntegrationTest})
 *   <li>RANDOM_PORT + OAuth group ({@link AppOAuthWebIntegrationTest})
 *   <li>{@code OrphanMediaCollectionScenarioTest} — a zero grace-period override
 *   <li>{@code GoogleSignInChainErrorWebTest} — a {@code @MockitoBean}
 *   <li>{@code ApiEdgeTest} — an extra {@code @Import}
 *   <li>{@code ResourceServerDecoderContractTest.WithIssuerUri} / {@code .WithJwkSetUri} — distinct
 *       resource-server config, one context each
 * </ul>
 *
 * floor + 1 leaves headroom for one deliberate new context; a second regression fails the build.
 */
public final class AppContextGuard extends DistinctContextGuard {

    @Override
    protected int limit() {
        return 9;
    }

    @Override
    protected String scope() {
        return "The app suite";
    }
}
