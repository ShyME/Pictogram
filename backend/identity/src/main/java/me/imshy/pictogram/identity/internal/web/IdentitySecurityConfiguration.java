package me.imshy.pictogram.identity.internal.web;

import me.imshy.pictogram.identity.internal.AuthProperties;
import me.imshy.pictogram.identity.internal.IdentityAuthentication;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

/**
 * identity's slice of the security filter chain (ADR-0004). Both chains are ordered ahead
 * of the {@code :app} resource-server chain so {@code /api/auth/**} and the OIDC endpoints
 * are handled here.
 *
 * <ul>
 *   <li>{@code /api/auth/**} — refresh and logout, authenticated by the refresh cookie alone,
 *       so the chain is open and stateless.</li>
 *   <li>{@code /oauth2/**}, {@code /login/**} — the backend-driven Authorization Code + PKCE
 *       handshake; Spring keeps the authorization request in a short-lived session, and
 *       {@link OidcSignInSuccessHandler} takes over on success. Locked down until a Google
 *       client is configured.</li>
 * </ul>
 */
@Configuration
class IdentitySecurityConfiguration {

    @Bean
    @Order(-2)
    SecurityFilterChain sessionEndpointsSecurity(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/auth/**")
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .build();
    }

    @Bean
    @Order(-1)
    SecurityFilterChain googleSignInSecurity(HttpSecurity http,
            ObjectProvider<ClientRegistrationRepository> clientRegistrations,
            ObjectProvider<OidcSignInSuccessHandler> successHandler) throws Exception {
        http.securityMatcher("/oauth2/**", "/login/**");
        if (clientRegistrations.getIfAvailable() == null) {
            return http.authorizeHttpRequests(requests -> requests.anyRequest().denyAll()).build();
        }
        return http
                .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                .oauth2Login(login -> login.successHandler(successHandler.getObject()))
                .build();
    }

    @Bean
    OidcSignInSuccessHandler oidcSignInSuccessHandler(IdentityAuthentication authentication,
            GoogleIdentityProvider google, AuthProperties properties) {
        return new OidcSignInSuccessHandler(authentication, google, properties);
    }
}
