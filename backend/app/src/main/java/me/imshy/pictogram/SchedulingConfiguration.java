package me.imshy.pictogram;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on {@code @Scheduled} for the deployable. Contributed at the composition root, like
 * the {@link ClockConfiguration one clock bean} — module tests boot without {@code :app}, so
 * their {@code @Scheduled} methods simply never fire.
 */
@Configuration
@EnableScheduling
class SchedulingConfiguration {
}
