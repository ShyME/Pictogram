package me.imshy.pictogram;

import java.time.Clock;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ModulithTestApplication {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
