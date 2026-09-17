package me.imshy.pictogram.shared.http;

import java.util.Optional;
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

public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if (!parameter.hasParameterAnnotation(CurrentUser.class)) {
            return false;
        }
        return isSupported(callerType(parameter));
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        boolean optional = Optional.class.equals(parameter.getParameterType());

        if (optional && !hasPictogramToken()) {
            return Optional.empty();
        }

        Object caller = as(callerType(parameter), currentUserId());
        return optional ? Optional.of(caller) : caller;
    }

    private static Class<?> callerType(MethodParameter parameter) {
        return Optional.class.equals(parameter.getParameterType())
                ? parameter.nestedIfOptional().getNestedParameterType()
                : parameter.getParameterType();
    }

    private static boolean isSupported(Class<?> type) {
        return type.equals(UserId.class) || type.equals(ViewerId.class);
    }

    private static Object as(Class<?> type, UserId userId) {
        return type.equals(ViewerId.class) ? ViewerId.of(userId) : userId;
    }

    private static boolean hasPictogramToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof Jwt;
    }

    private static UserId currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
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
