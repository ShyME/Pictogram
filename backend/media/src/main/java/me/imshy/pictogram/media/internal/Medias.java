package me.imshy.pictogram.media.internal;

import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

interface Medias extends CrudRepository<Media, UUID> {
}
