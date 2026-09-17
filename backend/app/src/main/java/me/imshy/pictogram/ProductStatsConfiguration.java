package me.imshy.pictogram;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ProductStatsProperties.class)
class ProductStatsConfiguration {
}
