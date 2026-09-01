package me.imshy.pictogram.follow.internal;

import java.util.List;
import java.util.UUID;
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
     * Removes the edge in one statement and reports how many rows went — {@code 0} when it
     * was already gone, so an unfollow of a non-followed user is a no-op with no event. In
     * its own transaction (the service is not {@code @Transactional}).
     */
    @Transactional
    int deleteByFollowerIdAndFollowedId(UUID followerId, UUID followedId);
}
