package me.imshy.pictogram.media;

import java.util.Optional;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.UserId;

/**
 * media's published interface (CONTEXT-MAP: post → media): does a media exist, and who
 * uploaded it. {@code post} calls {@link #ownerOf} to check the author of a new post owns
 * the media they are attaching.
 *
 * <p>Nothing about the bytes, the format, or the dimensions is exposed — those are
 * constants (ADR-0006) and the bytes are served straight from media's own endpoints.
 */
public interface MediaCatalog {

    boolean exists(MediaId mediaId);

    Optional<UserId> ownerOf(MediaId mediaId);
}
