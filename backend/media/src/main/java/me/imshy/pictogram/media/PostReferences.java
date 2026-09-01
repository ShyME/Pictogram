package me.imshy.pictogram.media;

import java.util.Collection;
import java.util.Set;
import me.imshy.pictogram.shared.MediaId;

/**
 * Tells media which of its media a live post still points at, so orphan collection never
 * deletes an image a post is using (media/CONTEXT.md: <em>Orphan</em>).
 *
 * <p>The knowledge belongs to {@code post} — it holds the {@code MediaId} on each row — but
 * the Gradle dependency runs {@code post → media} (CONTEXT-MAP), so media declares this port
 * and {@code post} provides the adapter. It is a query, not a command (ADR-0002).
 */
public interface PostReferences {

    /** The subset of {@code candidates} that an undeleted post references. */
    Set<MediaId> referencedAmong(Collection<MediaId> candidates);
}
