package me.imshy.pictogram.social.internal.likes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.social.PostLiked;
import me.imshy.pictogram.social.SocialEvent;
import me.imshy.pictogram.social.internal.SocialModuleIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.events.EventExternalizationConfiguration;
import org.springframework.modulith.test.Scenario;

/**
 * With the real wiring but no broker: a like publishes {@code PostLiked}, and
 * the container's {@link EventExternalizationConfiguration} — the bean the
 * Kafka relay uses in production — reshapes and routes it to
 * {@code pictogram.social} keyed by the post author.
 */
class LikeExternalizationTest extends SocialModuleIntegrationTest {

    @Autowired
    Liking liking;

    @Autowired
    EventExternalizationConfiguration externalization;

    private final ViewerId liker = ViewerId.random();
    private final UserId author = UserId.random();
    private final PostId post = PostId.random();

    @Test
    void aLikeIsPublishedAndExternalisedToTheRecipientKeyedTopic(Scenario scenario) {
        given(publishedPosts.authorOf(post)).willReturn(Optional.of(author));

        scenario.stimulate(() -> liking.like(liker, post)).andWaitForEventOfType(PostLiked.class)
            .toArriveAndVerify(liked -> {
                assertThat(externalization.supports(liked)).isTrue();
                assertThat(externalization.map(liked))
                    .isEqualTo(SocialEvent.postLiked(author, liker.asUserId(), post, liked.likedAt()));
                assertThat(externalization.determineTarget(liked).getKey()).isEqualTo(author.toString());
            });
    }
}
