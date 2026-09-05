package me.imshy.pictogram.social.internal.comment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

interface Comments extends CrudRepository<Comment, UUID> {

    @Query("""
        select new me.imshy.pictogram.social.internal.comment.CommentCount(c.postId, count(c))
        from Comment c
        where c.postId in :posts
        group by c.postId
        """)
    List<CommentCount> countsFor(@Param("posts") Collection<UUID> posts);

    @Transactional
    int deleteByPostId(UUID postId);

    @Query("""
        select c from Comment c
        where c.postId = :post
        order by c.createdAt asc, c.id asc
        """)
    List<Comment> oldestFor(@Param("post") UUID post, Limit limit);

    @Query("""
        select c from Comment c
        where c.postId = :post
          and (c.createdAt > :afterAt or (c.createdAt = :afterAt and c.id > :afterId))
        order by c.createdAt asc, c.id asc
        """)
    List<Comment> afterFor(@Param("post") UUID post, @Param("afterAt") Instant afterAt, @Param("afterId") UUID afterId,
        Limit limit);
}
