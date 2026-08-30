package me.imshy.pictogram.identity.internal;

import static org.mockito.BDDMockito.given;

import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * A module-integration test whose {@link Clock} the test body winds forward, so
 * token-lifetime assertions need no real waiting (ADR-0007).
 *
 * <p>The clock is a {@code @MockitoBean} delegating to a {@link MutableClock}: Spring
 * Modulith's module-test bean selector chokes on an {@code @Import}ed or nested
 * {@code @TestConfiguration} (spring-modulith#1381), and a mock bean has no factory method
 * for it to trip over.
 */
abstract class ClockControlledModuleTest extends IdentityModuleIntegrationTest {

    @MockitoBean
    Clock clock;

    final MutableClock time = MutableClock.at("2026-08-31T09:00:00Z");

    @BeforeEach
    void bindClockToMutableTime() {
        given(clock.instant()).willAnswer(invocation -> time.instant());
        given(clock.getZone()).willAnswer(invocation -> time.getZone());
    }
}
