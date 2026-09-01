package me.imshy.pictogram.follow.internal;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

interface Follows extends CrudRepository<Follow, UUID> {

    boolean existsByFollowerIdAndFollowedId(UUID followerId, UUID followedId);

    long countByFollowedId(UUID followedId);

    long countByFollowerId(UUID followerId);

    @Query("select f.followedId from Follow f where f.followerId = :follower")
    List<UUID> followedIdsOf(@Param("follower") UUID follower);

    /**
     * The follower / following list screens (#57) page newest-relationship-first —
     * {@code followedAt} descending, id as the keyset tiebreaker. Four queries, one first-
     * and one after-cursor per direction, mirroring {@code post}'s timeline reads. One
     * extra row is fetched (the {@link Limit}) so the caller can tell a further page exists
     * without a count.
     */
    @Query("""
            select f from Follow f
            where f.followedId = :user
            order by f.followedAt desc, f.id desc
            """)
    List<Follow> followersNewest(@Param("user") UUID user, Limit limit);

    @Query("""
            select f from Follow f
            where f.followedId = :user
              and (f.followedAt < :beforeAt or (f.followedAt = :beforeAt and f.id < :beforeId))
            order by f.followedAt desc, f.id desc
            """)
    List<Follow> followersBefore(@Param("user") UUID user,
            @Param("beforeAt") Instant beforeAt, @Param("beforeId") UUID beforeId, Limit limit);

    @Query("""
            select f from Follow f
            where f.followerId = :user
            order by f.followedAt desc, f.id desc
            """)
    List<Follow> followingNewest(@Param("user") UUID user, Limit limit);

    @Query("""
            select f from Follow f
            where f.followerId = :user
              and (f.followedAt < :beforeAt or (f.followedAt = :beforeAt and f.id < :beforeId))
            order by f.followedAt desc, f.id desc
            """)
    List<Follow> followingBefore(@Param("user") UUID user,
            @Param("beforeAt") Instant beforeAt, @Param("beforeId") UUID beforeId, Limit limit);

    /**
     * The batch relationship read (#59) does three fixed queries whatever the id-set size:
     * the follower count and the following count of every listed user, then which of them
     * the viewer follows. {@code followerId in} an empty collection is never called — the
     * service short-circuits an empty request.
     */
    @Query("""
            select new me.imshy.pictogram.follow.internal.FollowCount(f.followedId, count(f))
            from Follow f
            where f.followedId in :users
            group by f.followedId
            """)
    List<FollowCount> followerCounts(@Param("users") Collection<UUID> users);

    @Query("""
            select new me.imshy.pictogram.follow.internal.FollowCount(f.followerId, count(f))
            from Follow f
            where f.followerId in :users
            group by f.followerId
            """)
    List<FollowCount> followingCounts(@Param("users") Collection<UUID> users);

    @Query("select f.followedId from Follow f where f.followerId = :viewer and f.followedId in :users")
    List<UUID> followedByViewerAmong(@Param("viewer") UUID viewer, @Param("users") Collection<UUID> users);

    /**
     * Removes the edge in one statement and reports how many rows went — {@code 0} when it
     * was already gone, so an unfollow of a non-followed user is a no-op with no event. In
     * its own transaction (the service is not {@code @Transactional}).
     */
    @Transactional
    int deleteByFollowerIdAndFollowedId(UUID followerId, UUID followedId);
}
