package me.imshy.pictogram.profile.internal;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Time is an injectable {@link Clock} (ADR-0007). Each module contributes a fallback so it
 * still runs under {@code @ApplicationModuleTest}; {@code :app} and a controlled-time test
 * both supply their own, which wins via {@code @ConditionalOnMissingBean}.
 */
@Configuration
class ProfileConfiguration {

    @Bean
    @ConditionalOnMissingBean
    Clock profileClock() {
        return Clock.systemUTC();
    }
}
