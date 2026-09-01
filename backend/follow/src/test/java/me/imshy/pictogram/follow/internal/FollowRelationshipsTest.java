package me.imshy.pictogram.follow.internal;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import me.imshy.pictogram.follow.internal.FollowRelationships.Relationship;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.ViewerId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class FollowRelationshipsTest extends FollowModuleIntegrationTest {

    @Autowired
    Following following;

    @Autowired
    FollowRelationships relationships;

    @Test
    void reportsEachRequestedUsersCountsAndWhetherTheViewerFollowsThem() {
        var viewer = ViewerId.random();
        var bob = UserId.random();
        var carol = UserId.random();
        var dan = ViewerId.random();

        following.follow(viewer, bob);
        following.follow(dan, bob);
        following.follow(ViewerId.of(bob), carol);

        Map<UserId, Relationship> byId = index(relationships.of(viewer, List.of(bob, carol)));

        assertThat(byId.get(bob)).isEqualTo(new Relationship(bob, 2, 1, true));
        assertThat(byId.get(carol)).isEqualTo(new Relationship(carol, 1, 0, false));
    }

    @Test
    void returnsARecordForEveryRequestedIdIncludingOneWithNoEdges() {
        var stranger = UserId.random();

        assertThat(relationships.of(ViewerId.random(), List.of(stranger)))
                .containsExactly(new Relationship(stranger, 0, 0, false));
    }

    @Test
    void anEmptyRequestReturnsNothing() {
        assertThat(relationships.of(ViewerId.random(), List.of())).isEmpty();
    }

    @Test
    void theViewersOwnIdResolvesWithFollowedByViewerFalse() {
        var viewer = ViewerId.random();
        var other = UserId.random();
        following.follow(viewer, other);
        following.follow(ViewerId.of(other), viewer.asUserId());

        assertThat(relationships.of(viewer, List.of(viewer.asUserId())))
                .containsExactly(new Relationship(viewer.asUserId(), 1, 1, false));
    }

    private static Map<UserId, Relationship> index(List<Relationship> rows) {
        return rows.stream().collect(toMap(Relationship::user, identity()));
    }
}
