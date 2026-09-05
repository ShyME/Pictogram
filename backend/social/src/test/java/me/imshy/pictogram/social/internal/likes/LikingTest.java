package me.imshy.pictogram.social.internal.likes;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.social.LikeCounts.PostLikes;
import me.imshy.pictogram.social.PostLiked;
import me.imshy.pictogram.social.PostUnliked;
import me.imshy.pictogram.social.internal.SocialModuleIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class LikingTest extends SocialModuleIntegrationTest {

    @Autowired
    Liking liking;

    @Autowired
    LikeTally likeTally;

    @Autowired
    PlatformTransactionManager transactionManager;

    private final ViewerId ada = ViewerId.random();
    private final PostId post = PostId.random();

    @Test
    void likingAPostRecordsItAndAnnouncesIt(AssertablePublishedEvents events) {
        liking.like(ada, post);

        assertThat(likeStateFor(ada, post)).isEqualTo(new PostLikes(post, 1, true));
        assertThat(events.ofType(PostLiked.class)).singleElement().satisfies(event -> {
            assertThat(event.postId()).isEqualTo(post);
            assertThat(event.viewer()).isEqualTo(ada);
            assertThat(event.likedAt()).isNotNull();
        });
    }

    @Test
    void likingAPostAlreadyLikedChangesNothingAndAnnouncesNothing(AssertablePublishedEvents events) {
        liking.like(ada, post);

        liking.like(ada, post);

        assertThat(likeStateFor(ada, post).likeCount()).isEqualTo(1);
        assertThat(events.ofType(PostLiked.class)).hasSize(1);
    }

    @Test
    void unlikingRemovesTheLikeAndAnnouncesIt(AssertablePublishedEvents events) {
        liking.like(ada, post);

        liking.unlike(ada, post);

        assertThat(likeStateFor(ada, post)).isEqualTo(new PostLikes(post, 0, false));
        assertThat(events.ofType(PostUnliked.class)).singleElement().satisfies(event -> {
            assertThat(event.postId()).isEqualTo(post);
            assertThat(event.viewer()).isEqualTo(ada);
        });
    }

    @Test
    void unlikingAPostNotLikedChangesNothingAndAnnouncesNothing(AssertablePublishedEvents events) {
        liking.unlike(ada, post);

        assertThat(likeStateFor(ada, post)).isEqualTo(new PostLikes(post, 0, false));
        assertThat(events.ofType(PostUnliked.class)).isEmpty();
    }

    @Test
    void likeStaysIdempotentWhenACallerRunsItInsideTheirOwnTransaction(AssertablePublishedEvents events) {
        // like() checks existence before it saves, rather than relying only on the
        // DataIntegrityViolationException catch, precisely so a second like() can no-op
        // inside a caller's transaction — a constraint violation would doom that
        // transaction, catch or no catch.
        var inCallerTransaction = new TransactionTemplate(transactionManager);

        inCallerTransaction.executeWithoutResult(status -> {
            liking.like(ada, post);
            liking.like(ada, post);
        });

        assertThat(likeStateFor(ada, post)).isEqualTo(new PostLikes(post, 1, true));
        assertThat(events.ofType(PostLiked.class)).hasSize(1);
    }

    @Test
    void aViewerMayLikeTheirOwnPost() {
        var author = ViewerId.random();

        liking.like(author, post);

        assertThat(likeStateFor(author, post)).isEqualTo(new PostLikes(post, 1, true));
    }

    private PostLikes likeStateFor(ViewerId viewer, PostId post) {
        return likeTally.of(viewer, List.of(post)).getFirst();
    }
}
