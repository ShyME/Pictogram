package me.imshy.pictogram.social.internal.follow;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

interface Follows extends CrudRepository<Follow, FollowId> {

    boolean existsByFollowerIdAndFollowedId(UUID followerId, UUID followedId);

    long countByFollowedId(UUID followedId);

    long countByFollowerId(UUID followerId);

    @Query("select f.followedId from Follow f where f.followerId = :follower")
    List<UUID> followedIdsOf(@Param("follower") UUID follower);

    @Query("""
        select f from Follow f
        where f.followedId = :user
        order by f.followedAt desc, f.followerId desc
        """)
    List<Follow> followersNewest(@Param("user") UUID user, Limit limit);

    @Query("""
        select f from Follow f
        where f.followedId = :user
          and (f.followedAt < :beforeAt or (f.followedAt = :beforeAt and f.followerId < :beforeId))
        order by f.followedAt desc, f.followerId desc
        """)
    List<Follow> followersBefore(@Param("user") UUID user, @Param("beforeAt") Instant beforeAt,
        @Param("beforeId") UUID beforeId, Limit limit);

    @Query("""
        select f from Follow f
        where f.followerId = :user
        order by f.followedAt desc, f.followedId desc
        """)
    List<Follow> followingNewest(@Param("user") UUID user, Limit limit);

    @Query("""
        select f from Follow f
        where f.followerId = :user
          and (f.followedAt < :beforeAt or (f.followedAt = :beforeAt and f.followedId < :beforeId))
        order by f.followedAt desc, f.followedId desc
        """)
    List<Follow> followingBefore(@Param("user") UUID user, @Param("beforeAt") Instant beforeAt,
        @Param("beforeId") UUID beforeId, Limit limit);

    @Query("""
        select new me.imshy.pictogram.social.internal.follow.FollowCount(f.followedId, count(f))
        from Follow f
        where f.followedId in :users
        group by f.followedId
        """)
    List<FollowCount> followerCounts(@Param("users") Collection<UUID> users);

    @Query("""
        select new me.imshy.pictogram.social.internal.follow.FollowCount(f.followerId, count(f))
        from Follow f
        where f.followerId in :users
        group by f.followerId
        """)
    List<FollowCount> followingCounts(@Param("users") Collection<UUID> users);

    @Query("select f.followedId from Follow f where f.followerId = :viewer and f.followedId in :users")
    List<UUID> followedByViewerAmong(@Param("viewer") UUID viewer, @Param("users") Collection<UUID> users);

    @Transactional
    int deleteByFollowerIdAndFollowedId(UUID followerId, UUID followedId);
}
