package me.imshy.pictogram;

import me.imshy.pictogram.shared.http.ProblemDetailAccessDeniedHandler;
import me.imshy.pictogram.shared.http.ProblemDetailAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
class ApiSecurityConfiguration {

    @Bean
    @Order(1)
    SecurityFilterChain apiSecurity(
            HttpSecurity http,
            JwtDecoder identityJwtDecoder,
            ProblemDetailAuthenticationEntryPoint entryPoint,
            ProblemDetailAccessDeniedHandler accessDeniedHandler)
            throws Exception {
        return http.securityMatcher("/api/**")
                .authorizeHttpRequests(requests -> requests.requestMatchers(HttpMethod.GET, "/api/profiles/me")
                        .authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/profiles/*")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/media/*/original", "/api/media/*/thumbnail")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/posts")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/follows/*")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/likes")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                        .jwt(jwt -> jwt.decoder(identityJwtDecoder)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .headers(WebSecurityHeaders::apply)
                .exceptionHandling(handling ->
                        handling.authenticationEntryPoint(entryPoint).accessDeniedHandler(accessDeniedHandler))
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain openEndpoints(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .headers(WebSecurityHeaders::apply)
                .build();
    }
}
