package me.imshy.pictogram;

import java.time.Clock;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * The {@code @SpringBootConfiguration} a single-module {@code @ApplicationModuleTest} boots
 * against, so a bounded-context subproject need not depend on {@code :app}.
 *
 * <p>It carries the {@link Clock} bean for module tests, matching {@code :app}'s
 * {@code ClockConfiguration} (issue #29): {@code @ApplicationModuleTest} narrows component
 * scanning to the module under test, and an {@code @Import}ed factory is not resolved either
 * (spring-modulith#1381), so this root config — which every module test already boots — is
 * the one place the bean can live. A controlled-time test overrides it with
 * {@code @MockitoBean Clock}.
 */
@SpringBootApplication
public class ModulithTestApplication {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
