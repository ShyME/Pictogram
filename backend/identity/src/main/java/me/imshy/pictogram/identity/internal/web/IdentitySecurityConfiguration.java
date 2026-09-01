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
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

/**
 * identity's slice of the security filter chain (ADR-0004). Both chains are ordered ahead
 * of the {@code :app} resource-server chain so {@code /api/auth/**} and the OIDC endpoints
 * are handled here.
 *
 * <ul>
 *   <li>{@code /api/auth/**} — refresh and logout, authenticated by the refresh cookie alone,
 *       so the chain is open and stateless.</li>
 *   <li>{@code /oauth2/**}, {@code /login/oauth2/**} — the backend-driven Authorization Code
 *       + PKCE handshake; Spring keeps the authorization request in a short-lived session,
 *       and {@link OidcSignInSuccessHandler} takes over on success. The matcher stops at
 *       {@code /login/oauth2/**} (the redirection endpoint) rather than all of
 *       {@code /login/**} so the SPA's own {@code /login} route falls through to the
 *       permit-all chain (#34). Locked down until a Google client is configured.</li>
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
            ObjectProvider<OidcSignInSuccessHandler> successHandler,
            SignInCompletion completion, ObjectProvider<ObjectMapper> objectMapper) throws Exception {
        http.securityMatcher("/oauth2/**", "/login/oauth2/**");
        if (clientRegistrations.getIfAvailable() == null) {
            return http.authorizeHttpRequests(requests -> requests.anyRequest().denyAll()).build();
        }
        return http
                .addFilterBefore(new OidcChainErrorFilter(objectMapper.getObject(), completion),
                        OAuth2AuthorizationRequestRedirectFilter.class)
                .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                .oauth2Login(login -> login
                        .successHandler(successHandler.getObject())
                        .failureHandler(new SignInFailureHandler(completion)))
                .build();
    }

    @Bean
    SignInCompletion signInCompletion(AuthProperties properties) {
        return new SignInCompletion(properties);
    }

    @Bean
    OidcSignInSuccessHandler oidcSignInSuccessHandler(IdentityAuthentication authentication,
            GoogleIdentityProvider google, SignInCompletion completion) {
        return new OidcSignInSuccessHandler(authentication, google, completion);
    }
}
