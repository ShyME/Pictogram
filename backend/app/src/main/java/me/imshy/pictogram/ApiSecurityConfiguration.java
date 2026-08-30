package me.imshy.pictogram;

import me.imshy.pictogram.shared.http.ProblemDetailAccessDeniedHandler;
import me.imshy.pictogram.shared.http.ProblemDetailAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The API runs as a stateless OAuth2 resource server: every {@code /api/**} request must
 * carry a valid Pictogram access token, verified with the {@code JwtDecoder} the identity
 * module contributes (ADR-0004). Both rejection paths render as Problem Details. Everything
 * else — the actuator probes, the bundled SPA, and identity's own sign-in endpoints — is
 * handled by other filter chains.
 */
@Configuration
@EnableWebSecurity
class ApiSecurityConfiguration {

    @Bean
    @Order(1)
    SecurityFilterChain apiSecurity(HttpSecurity http,
            ProblemDetailAuthenticationEntryPoint entryPoint,
            ProblemDetailAccessDeniedHandler accessDeniedHandler) throws Exception {
        return http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                        .jwt(jwt -> {
                        }))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain openEndpoints(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .build();
    }
}
