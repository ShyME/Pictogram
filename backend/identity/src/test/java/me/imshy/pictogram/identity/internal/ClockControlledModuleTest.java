package me.imshy.pictogram.identity.internal;

import static org.mockito.BDDMockito.given;

import java.time.Clock;
import me.imshy.pictogram.testsupport.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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
