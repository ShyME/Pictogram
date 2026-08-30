package me.imshy.pictogram;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

/** The modular monolith. {@code shared} is whitelisted so any context may use its ID types. */
@Modulithic(systemName = "Pictogram", sharedModules = "shared")
@SpringBootApplication
public class PictogramApplication {

    static void main(String[] args) {
        SpringApplication.run(PictogramApplication.class, args);
    }
}
