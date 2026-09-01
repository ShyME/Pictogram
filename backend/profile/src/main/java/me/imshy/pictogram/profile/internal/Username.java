package me.imshy.pictogram.profile.internal;

import java.util.regex.Pattern;

record Username(String value) {

    private static final Pattern SHAPE = Pattern.compile("^[a-z0-9_]{3,20}$");

    Username {
        if (value == null || !SHAPE.matcher(value).matches()) {
            throw new MalformedUsernameException();
        }
    }
}
