package me.imshy.pictogram;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import me.imshy.pictogram.shared.http.CurrentUser;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfiguration {

    static {
        SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentUser.class);
    }

    @Bean
    OpenAPI pictogramOpenApi() {
        return new OpenAPI()
            .info(new Info().title("Pictogram API").version("v1").description("The HTTP API the Pictogram SPA drives."))
            .servers(List.of(new Server().url("/")));
    }
}
