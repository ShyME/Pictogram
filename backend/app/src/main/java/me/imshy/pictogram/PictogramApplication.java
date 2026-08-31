package me.imshy.pictogram;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.modulith.Modulithic;

/** The modular monolith. {@code shared} is whitelisted so any context may use its ID types. */
@Modulithic(systemName = "Pictogram", sharedModules = "shared")
@SpringBootApplication
// test-support's ModulithTestApplication sits at this package root (so @ApplicationModuleTest
// finds it) and is on :app's test classpath, so this component scan would otherwise pick up
// its module-test-only Clock bean and clash with ClockConfiguration (issue #29). The two
// CUSTOM filters are @SpringBootApplication's own defaults, restated because declaring
// @ComponentScan here replaces the meta-annotated one.
@ComponentScan(excludeFilters = {
        @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
        @Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class),
        @Filter(type = FilterType.REGEX, pattern = "me\\.imshy\\.pictogram\\.ModulithTestApplication")
})
public class PictogramApplication {

    static void main(String[] args) {
        SpringApplication.run(PictogramApplication.class, args);
    }
}
