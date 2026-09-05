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

    @Query("select distinct p.mediaId from Post p where p.mediaId in :mediaIds")
    List<UUID> mediaIdsAmong(@Param("mediaIds") Collection<UUID> mediaIds);

    @Query("""
        select p from Post p
        where p.authorId = :author
        order by p.publishedAt desc, p.id desc
        """)
    List<Post> newestBy(@Param("author") UUID author, Limit limit);

    @Query("""
        select p from Post p
        where p.authorId = :author
          and (p.publishedAt < :beforePublishedAt
               or (p.publishedAt = :beforePublishedAt and p.id < :beforeId))
        order by p.publishedAt desc, p.id desc
        """)
    List<Post> pageBy(@Param("author") UUID author, @Param("beforePublishedAt") Instant beforePublishedAt,
        @Param("beforeId") UUID beforeId, Limit limit);

    @Query("""
        select p from Post p
        where p.authorId in :authors
        order by p.publishedAt desc, p.id desc
        """)
    List<Post> newestByAuthors(@Param("authors") Collection<UUID> authors, Limit limit);

    @Query("""
        select p from Post p
        where p.authorId in :authors
          and (p.publishedAt < :beforePublishedAt
               or (p.publishedAt = :beforePublishedAt and p.id < :beforeId))
        order by p.publishedAt desc, p.id desc
        """)
    List<Post> byAuthorsBefore(@Param("authors") Collection<UUID> authors,
        @Param("beforePublishedAt") Instant beforePublishedAt, @Param("beforeId") UUID beforeId, Limit limit);
}
