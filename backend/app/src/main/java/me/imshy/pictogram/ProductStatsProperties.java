package me.imshy.pictogram;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("pictogram.stats")
record ProductStatsProperties(@DefaultValue("24h") Duration activeWindow) {}
