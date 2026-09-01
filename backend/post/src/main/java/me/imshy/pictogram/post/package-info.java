/**
 * The {@code Post}: one image (a {@code MediaId}) plus an optional caption, published by an
 * author. Immutable once published — only created or deleted. Emits {@code PostPublished} /
 * {@code PostDeleted}.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Post")
package me.imshy.pictogram.post;
