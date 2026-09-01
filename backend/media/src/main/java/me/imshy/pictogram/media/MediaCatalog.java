package me.imshy.pictogram.media;

import java.util.Optional;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.UserId;

public interface MediaCatalog {

    boolean exists(MediaId mediaId);

    Optional<UserId> ownerOf(MediaId mediaId);
}
