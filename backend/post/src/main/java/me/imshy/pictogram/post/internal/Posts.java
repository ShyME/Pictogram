package me.imshy.pictogram.post.internal;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

interface Posts extends CrudRepository<Post, UUID> {

    /** Of {@code mediaIds}, the distinct ones some post still references — media's orphan check (#16). */
    @Query("select distinct p.mediaId from Post p where p.mediaId in :mediaIds")
    List<UUID> mediaIdsAmong(@Param("mediaIds") Collection<UUID> mediaIds);

    /** The newest page of an author's posts — {@code publishedAt} descending, id as tiebreaker. */
    @Query("""
            select p from Post p
            where p.authorId = :author
            order by p.publishedAt desc, p.id desc
            """)
    List<Post> newestBy(@Param("author") UUID author, Limit limit);

    /** The next page, strictly older than the {@code (publishedAt, id)} the cursor names. */
    @Query("""
            select p from Post p
            where p.authorId = :author
              and (p.publishedAt < :beforePublishedAt
                   or (p.publishedAt = :beforePublishedAt and p.id < :beforeId))
            order by p.publishedAt desc, p.id desc
            """)
    List<Post> pageBy(@Param("author") UUID author,
            @Param("beforePublishedAt") Instant beforePublishedAt,
            @Param("beforeId") UUID beforeId,
            Limit limit);
}
