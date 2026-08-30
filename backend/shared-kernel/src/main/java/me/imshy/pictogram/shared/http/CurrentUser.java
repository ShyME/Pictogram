package me.imshy.pictogram.shared.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a controller method parameter of type {@code UserId} or {@code ViewerId} to the
 * caller resolved from the Pictogram access token at the edge (spec §API — never an
 * ambient lookup inside a service). Resolved by {@link CurrentUserArgumentResolver}.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
