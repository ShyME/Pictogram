package me.imshy.pictogram;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import me.imshy.pictogram.shared.http.CurrentUser;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Shapes the document so {@code openapi.json} is deterministic and only changes when the API
 * changes: a fixed title/version (springdoc otherwise stamps in the build version) and a
 * relative server URL (otherwise the random test port leaks in). {@code @CurrentUser}
 * arguments are resolved from the token, not the request — hidden so they don't surface as
 * bogus query parameters.
 */
@Configuration
class OpenApiConfiguration {

    static {
        SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentUser.class);
    }

    @Bean
    OpenAPI pictogramOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Pictogram API")
                        .version("v1")
                        .description("The HTTP API the Pictogram SPA drives."))
                .servers(List.of(new Server().url("/")));
    }
}
