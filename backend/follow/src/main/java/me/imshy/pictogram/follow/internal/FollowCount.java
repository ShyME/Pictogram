package me.imshy.pictogram.follow.internal;

import java.util.UUID;

/** A constructor-expression target — JPQL can't return a {@code Map} directly. */
record FollowCount(UUID userId, long count) {
}
