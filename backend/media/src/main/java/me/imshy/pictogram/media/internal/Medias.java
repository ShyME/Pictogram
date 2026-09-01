package me.imshy.pictogram.media.internal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

interface Medias extends CrudRepository<Media, UUID> {

    @Query("select m from Media m where m.createdAt < :cutoff order by m.createdAt asc")
    List<Media> uploadedBefore(@Param("cutoff") Instant cutoff, Limit limit);
}
