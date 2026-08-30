package me.imshy.pictogram;

import me.imshy.pictogram.shared.http.ProblemDetailAccessDeniedHandler;
import me.imshy.pictogram.shared.http.ProblemDetailAuthenticationEntryPoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The API runs as a stateless OAuth2 resource server: every {@code /api/**} request must
 * carry a valid Pictogram access token (ADR-0004), and both rejection paths render as
 * Problem Details. Everything else — the actuator probes, the bundled SPA — stays open.
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

    /**
     * Placeholder verifier until the identity slice (#8) mints Pictogram access tokens and
     * publishes their EdDSA verification key. Every presented token is rejected, so
     * {@code /api} is uniformly 401 until then; a real {@link JwtDecoder} bean (or the
     * {@code spring.security.oauth2.resourceserver.jwt.*} properties) supersedes it.
     */
    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    JwtDecoder unconfiguredJwtDecoder() {
        return token -> {
            throw new BadJwtException("No Pictogram access-token verification key is configured yet");
        };
    }
}
