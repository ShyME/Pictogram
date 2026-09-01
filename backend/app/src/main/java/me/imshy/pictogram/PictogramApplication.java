package me.imshy.pictogram;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.modulith.Modulithic;

@Modulithic(systemName = "Pictogram", sharedModules = "shared")
@SpringBootApplication
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
