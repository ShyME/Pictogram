package me.imshy.pictogram.social.internal.likes;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

interface Likes extends CrudRepository<Like, LikeId> {

    boolean existsByPostIdAndViewerId(UUID postId, UUID viewerId);

    @Transactional
    int deleteByPostIdAndViewerId(UUID postId, UUID viewerId);

    @Query("""
            select new me.imshy.pictogram.social.internal.likes.LikeCount(l.postId, count(l))
            from Like l
            where l.postId in :posts
            group by l.postId
            """)
    List<LikeCount> countsFor(@Param("posts") Collection<UUID> posts);

    @Query("select l.postId from Like l where l.viewerId = :viewer and l.postId in :posts")
    List<UUID> likedByViewerAmong(@Param("viewer") UUID viewer, @Param("posts") Collection<UUID> posts);
}
