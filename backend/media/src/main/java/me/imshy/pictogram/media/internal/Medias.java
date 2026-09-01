package me.imshy.pictogram.media.internal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

interface Medias extends CrudRepository<Media, UUID> {

    /**
     * The orphan sweep's candidates: oldest uploads first, capped at {@code limit}. Scans
     * {@code media} with no index on {@code created_at} — fine at v1 scale, and adding one
     * now would be an out-of-order migration under ADR-0009's module-ordinal scheme.
     */
    @Query("select m from Media m where m.createdAt < :cutoff order by m.createdAt asc")
    List<Media> uploadedBefore(@Param("cutoff") Instant cutoff, Limit limit);
}
