package me.imshy.pictogram.shared.http;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("pictogram.ratelimit.write")
record RateLimitProperties(@DefaultValue("20") int capacity, @DefaultValue("1m") Duration window) {
}
