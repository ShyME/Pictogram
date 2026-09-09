package me.imshy.pictogram.social.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import me.imshy.pictogram.post.PublishedPosts;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.social.CommentDeleted;
import me.imshy.pictogram.social.PostCommented;
import me.imshy.pictogram.social.PostLiked;
import me.imshy.pictogram.social.PostUnliked;
import me.imshy.pictogram.social.SocialEvent;
import me.imshy.pictogram.social.UserFollowed;
import me.imshy.pictogram.social.UserUnfollowed;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.modulith.events.EventExternalizationConfiguration;

/**
 * The producer half of ADR-0015, proved with no broker: the three engagement
 * events are selected for {@code pictogram.social}, reshaped to
 * {@link SocialEvent}, and keyed by the recipient — the post author (via
 * {@link PublishedPosts#authorOf}) for a like or comment, the followed user for
 * a follow.
 */
class SocialEventExternalizationTest {

    private final PublishedPosts publishedPosts = Mockito.mock(PublishedPosts.class);
    private final EventExternalizationConfiguration externalization = new SocialEventExternalization()
        .pictogramSocialExternalization(publishedPosts);

    private final UserId author = UserId.random();
    private final ViewerId actor = ViewerId.random();
    private final PostId post = PostId.random();
    private final Instant at = Instant.parse("2026-09-09T12:00:00Z");

    @Test
    void aLikeIsRoutedToTheSocialTopicKeyedByThePostAuthor() {
        given(publishedPosts.authorOf(post)).willReturn(Optional.of(author));
        var liked = new PostLiked(post, actor, at);

        assertThat(externalization.supports(liked)).isTrue();
        assertThat(externalization.map(liked)).isEqualTo(SocialEvent.postLiked(author, actor.asUserId(), post, at));

        var target = externalization.determineTarget(liked);
        assertThat(target.getTarget()).isEqualTo("pictogram.social");
        assertThat(target.getKey()).isEqualTo(author.toString());
    }

    @Test
    void aCommentIsRoutedTheSameWayAsALike() {
        given(publishedPosts.authorOf(post)).willReturn(Optional.of(author));
        var commented = new PostCommented(post, UUID.randomUUID(), actor, at);

        assertThat(externalization.supports(commented)).isTrue();
        assertThat(externalization.map(commented))
            .isEqualTo(SocialEvent.postCommented(author, actor.asUserId(), post, at));
        assertThat(externalization.determineTarget(commented).getKey()).isEqualTo(author.toString());
    }

    @Test
    void aFollowIsRoutedKeyedByTheFollowedUserWithNoPostAuthorLookup() {
        var followed = UserId.random();
        var follower = UserId.random();
        var event = new UserFollowed(follower, followed, at);

        assertThat(externalization.supports(event)).isTrue();
        assertThat(externalization.map(event)).isEqualTo(SocialEvent.userFollowed(followed, follower, at));
        assertThat(externalization.determineTarget(event).getKey()).isEqualTo(followed.toString());

        Mockito.verifyNoInteractions(publishedPosts);
    }

    @Test
    void aLikeOnAPostWhoseAuthorCannotBeResolvedIsNotExternalised() {
        given(publishedPosts.authorOf(post)).willReturn(Optional.empty());

        assertThat(externalization.supports(new PostLiked(post, actor, at))).isFalse();
    }

    @Test
    void theUndoAndLifecycleEventsAreNotExternalised() {
        assertThat(externalization.supports(new PostUnliked(post, actor, at))).isFalse();
        assertThat(externalization.supports(new UserUnfollowed(actor.asUserId(), author, at))).isFalse();
        assertThat(externalization.supports(new CommentDeleted(post, UUID.randomUUID(), actor, at))).isFalse();
    }
}
