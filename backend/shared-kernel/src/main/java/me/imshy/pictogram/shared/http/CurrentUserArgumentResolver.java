package me.imshy.pictogram.shared.http;

import java.util.UUID;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.ViewerId;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Turns the authenticated Pictogram access token into an explicit {@link UserId} or
 * {@link ViewerId} parameter, so authorization is visible at the edge rather than an
 * ambient {@code SecurityContext} read buried in a service (spec §API).
 */
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if (!parameter.hasParameterAnnotation(CurrentUser.class)) {
            return false;
        }
        var type = parameter.getParameterType();
        return type.equals(UserId.class) || type.equals(ViewerId.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        var userId = currentUserId();
        return parameter.getParameterType().equals(ViewerId.class) ? ViewerId.of(userId) : userId;
    }

    private static UserId currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new UnauthenticatedException("The request carries no Pictogram access token.");
        }
        var subject = jwt.getSubject();
        if (subject == null) {
            throw new UnauthenticatedException("The access token has no subject claim.");
        }
        try {
            return new UserId(UUID.fromString(subject));
        } catch (IllegalArgumentException notAUuid) {
            throw new UnauthenticatedException("The access token subject is not a Pictogram user id.");
        }
    }
}
