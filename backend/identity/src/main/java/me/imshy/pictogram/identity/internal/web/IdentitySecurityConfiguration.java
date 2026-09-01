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

@Configuration
class IdentitySecurityConfiguration {

    @Bean
    @Order(-2)
    SecurityFilterChain sessionEndpointsSecurity(HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/auth/**")
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .build();
    }

    @Bean
    @Order(-1)
    SecurityFilterChain googleSignInSecurity(
            HttpSecurity http,
            ObjectProvider<ClientRegistrationRepository> clientRegistrations,
            ObjectProvider<OidcSignInSuccessHandler> successHandler,
            SignInCompletion completion,
            ObjectProvider<ObjectMapper> objectMapper)
            throws Exception {
        http.securityMatcher("/oauth2/**", "/login/oauth2/**");
        if (clientRegistrations.getIfAvailable() == null) {
            return http.authorizeHttpRequests(requests -> requests.anyRequest().denyAll())
                    .build();
        }
        return http.addFilterBefore(
                        new OidcChainErrorFilter(objectMapper.getObject(), completion),
                        OAuth2AuthorizationRequestRedirectFilter.class)
                .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                .oauth2Login(login -> login.successHandler(successHandler.getObject())
                        .failureHandler(new SignInFailureHandler(completion)))
                .build();
    }

    @Bean
    SignInCompletion signInCompletion(AuthProperties properties) {
        return new SignInCompletion(properties);
    }

    @Bean
    OidcSignInSuccessHandler oidcSignInSuccessHandler(
            IdentityAuthentication authentication, GoogleIdentityProvider google, SignInCompletion completion) {
        return new OidcSignInSuccessHandler(authentication, google, completion);
    }
}
