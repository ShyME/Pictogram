package me.imshy.pictogram.shared.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.ViewerId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.ReflectionUtils;

class CurrentUserArgumentResolverTest {

    private final CurrentUserArgumentResolver resolver = new CurrentUserArgumentResolver();
    private final UUID subject = UUID.randomUUID();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @SuppressWarnings("unused")
    void sample(@CurrentUser UserId userId,
            @CurrentUser ViewerId viewerId,
            @CurrentUser Optional<ViewerId> maybeViewer,
            @CurrentUser Optional<UserId> maybeUser,
            String notAnnotated) {
    }

    private MethodParameter parameter(int index) {
        Method method = ReflectionUtils.findMethod(CurrentUserArgumentResolverTest.class, "sample",
                UserId.class, ViewerId.class, Optional.class, Optional.class, String.class);
        return new MethodParameter(method, index);
    }

    private void signedInAs(UUID userId) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(userId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, AuthorityUtils.NO_AUTHORITIES));
    }

    private void anonymous() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
    }

    @Test
    void supportsAnnotatedUserIdViewerIdAndOptionalsOfThem() {
        assertThat(resolver.supportsParameter(parameter(0))).isTrue();
        assertThat(resolver.supportsParameter(parameter(1))).isTrue();
        assertThat(resolver.supportsParameter(parameter(2))).isTrue();
        assertThat(resolver.supportsParameter(parameter(3))).isTrue();
        assertThat(resolver.supportsParameter(parameter(4))).isFalse();
    }

    @Test
    void resolvesTheTokenSubjectAsUserIdAndViewerId() throws Exception {
        signedInAs(subject);

        assertThat(resolver.resolveArgument(parameter(0), null, null, null))
                .isEqualTo(new UserId(subject));
        assertThat(resolver.resolveArgument(parameter(1), null, null, null))
                .isEqualTo(new ViewerId(subject));
    }

    @Test
    void resolvesAnOptionalToThePresentViewerWhenSignedIn() throws Exception {
        signedInAs(subject);

        assertThat(resolver.resolveArgument(parameter(2), null, null, null))
                .isEqualTo(Optional.of(new ViewerId(subject)));
    }

    @Test
    void resolvesAnOptionalToEmptyWhenAnonymous() throws Exception {
        anonymous();

        assertThat(resolver.resolveArgument(parameter(2), null, null, null))
                .isEqualTo(Optional.empty());
        assertThat(resolver.resolveArgument(parameter(3), null, null, null))
                .isEqualTo(Optional.empty());
    }

    @Test
    void failsARequiredParameterWhenThereIsNoToken() {
        anonymous();

        assertThatExceptionOfType(UnauthenticatedException.class)
                .isThrownBy(() -> resolver.resolveArgument(parameter(1), null, null, null));
    }
}
