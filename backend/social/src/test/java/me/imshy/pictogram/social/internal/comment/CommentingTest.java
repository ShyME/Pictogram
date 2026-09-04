package me.imshy.pictogram.social.internal.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.Instant;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.social.PostCommented;
import me.imshy.pictogram.social.internal.SocialModuleIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class CommentingTest extends SocialModuleIntegrationTest {

    @Autowired
    Commenting commenting;

    @Autowired
    CommentThread thread;

    @MockitoBean
    Clock clock;

    private final Instant now = Instant.parse("2026-09-04T12:00:00Z");
    private final ViewerId ada = ViewerId.random();
    private final PostId post = PostId.random();

    @BeforeEach
    void bindClock() {
        given(clock.instant()).willReturn(now);
    }

    @Test
    void aCommentIsStoredAndReturnedWithItsGeneratedIdAndTimestamp(AssertablePublishedEvents events) {
        PostComment written = commenting.comment(ada, post, "first!");

        assertThat(written.commentId()).isNotNull();
        assertThat(written.postId()).isEqualTo(post);
        assertThat(written.viewer()).isEqualTo(ada);
        assertThat(written.body()).isEqualTo("first!");
        assertThat(written.createdAt()).isEqualTo(now);

        assertThat(thread.pageFor(post, null, null).comments()).singleElement().isEqualTo(written);

        assertThat(events.ofType(PostCommented.class)).singleElement().satisfies(event -> {
            assertThat(event.postId()).isEqualTo(post);
            assertThat(event.commentId()).isEqualTo(written.commentId());
            assertThat(event.viewer()).isEqualTo(ada);
            assertThat(event.commentedAt()).isEqualTo(now);
        });
    }

    @Test
    void everyNewCommentAnnouncesItsOwnEvent(AssertablePublishedEvents events) {
        commenting.comment(ada, post, "one");
        commenting.comment(ada, post, "two");

        assertThat(events.ofType(PostCommented.class)).hasSize(2);
    }

    @Test
    void theBodyIsTrimmed() {
        PostComment written = commenting.comment(ada, post, "  spaced out  ");

        assertThat(written.body()).isEqualTo("spaced out");
    }

    @Test
    void aBlankBodyIsRejected() {
        assertThatExceptionOfType(EmptyCommentException.class).isThrownBy(() -> commenting.comment(ada, post, "   "));
    }

    @Test
    void aBodyOverAThousandCharactersIsRejected() {
        String tooLong = "x".repeat(CommentBody.MAX_LENGTH + 1);

        assertThatExceptionOfType(CommentTooLongException.class)
                .isThrownBy(() -> commenting.comment(ada, post, tooLong));
    }

    @Test
    void aBodyOfExactlyAThousandCharactersIsAccepted() {
        String atLimit = "x".repeat(CommentBody.MAX_LENGTH);

        assertThat(commenting.comment(ada, post, atLimit).body()).hasSize(CommentBody.MAX_LENGTH);
    }

    @Test
    void commentingOnYourOwnPostIsAllowed() {
        // The comment sub-domain has no notion of a post's author — mirrors likes, contrast
        // the self-follow guard in follow.
        assertThat(commenting.comment(ada, new PostId(ada.value()), "talking to myself"))
                .isNotNull();
    }
}
