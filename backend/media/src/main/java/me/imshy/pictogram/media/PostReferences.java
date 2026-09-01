package me.imshy.pictogram.media;

import java.util.Collection;
import java.util.Set;
import me.imshy.pictogram.shared.MediaId;

public interface PostReferences {

    Set<MediaId> referencedAmong(Collection<MediaId> candidates);
}
