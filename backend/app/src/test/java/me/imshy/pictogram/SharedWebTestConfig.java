package me.imshy.pictogram;

import me.imshy.pictogram.testsupport.SharedMinio;
import me.imshy.pictogram.testsupport.SharedPostgres;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;

@TestConfiguration(proxyBeanMethods = false)
class SharedWebTestConfig {

    @Bean
    DynamicPropertyRegistrar datasourceProperties() {
        return SharedPostgres::registerTo;
    }

    @Bean
    DynamicPropertyRegistrar objectStorageProperties() {
        return SharedMinio::registerTo;
    }

    @Bean
    DynamicPropertyRegistrar authCookieProperties() {
        return registry -> registry.add("pictogram.auth.cookie-secure", () -> false);
    }
}
