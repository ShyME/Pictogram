package me.imshy.pictogram;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The one runtime {@link Clock} bean (ADR-0007), contributed at the composition root instead
 * of by a {@code @ConditionalOnMissingBean} fallback copied into every module (issue #29).
 * Module tests boot without {@code :app}, so {@code test-support}'s {@code ModulithTestApplication}
 * carries the equivalent bean for {@code @ApplicationModuleTest}.
 */
@Configuration
class ClockConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
