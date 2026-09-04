package me.imshy.pictogram.social.internal.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.shared.http.ForbiddenException;
import me.imshy.pictogram.social.CommentDeleted;
import me.imshy.pictogram.social.internal.SocialModuleIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class CommentRemovalTest extends SocialModuleIntegrationTest {

    @Autowired
    Commenting commenting;

    @Autowired
    CommentThread thread;

    @Autowired
    CommentRemoval removal;

    @MockitoBean
    Clock clock;

    private final Instant now = Instant.parse("2026-09-04T12:00:00Z");
    private final ViewerId author = ViewerId.random();
    private final ViewerId postAuthor = ViewerId.random();
    private final PostId post = PostId.random();

    @BeforeEach
    void bindClock() {
        given(clock.instant()).willReturn(now);
        given(publishedPosts.authorOf(post)).willReturn(Optional.of(new UserId(postAuthor.value())));
    }

    @Test
    void theCommentsAuthorCanDeleteItAndTheDeletionIsAnnounced(AssertablePublishedEvents events) {
        UUID commentId = commenting.comment(author, post, "my mistake").commentId();

        removal.delete(author, commentId);

        assertThat(thread.pageFor(post, null, null).comments()).isEmpty();
        assertThat(events.ofType(CommentDeleted.class)).singleElement().satisfies(event -> {
            assertThat(event.postId()).isEqualTo(post);
            assertThat(event.commentId()).isEqualTo(commentId);
            assertThat(event.viewer()).isEqualTo(author);
            assertThat(event.deletedAt()).isEqualTo(now);
        });
    }

    @Test
    void thePostsAuthorCanDeleteSomeoneElsesCommentAndTheEventNamesThem(AssertablePublishedEvents events) {
        UUID commentId = commenting.comment(author, post, "not your call").commentId();

        removal.delete(postAuthor, commentId);

        assertThat(thread.pageFor(post, null, null).comments()).isEmpty();
        assertThat(events.ofType(CommentDeleted.class))
                .singleElement()
                .satisfies(event -> assertThat(event.viewer()).isEqualTo(postAuthor));
    }

    @Test
    void aStrangerCannotDeleteAndTheCommentStands(AssertablePublishedEvents events) {
        UUID commentId = commenting.comment(author, post, "leave it").commentId();

        assertThatExceptionOfType(ForbiddenException.class)
                .isThrownBy(() -> removal.delete(ViewerId.random(), commentId));

        assertThat(thread.pageFor(post, null, null).comments()).hasSize(1);
        assertThat(events.ofType(CommentDeleted.class)).isEmpty();
    }

    @Test
    void deletingACommentThatIsNotThereIsASilentNoOp(AssertablePublishedEvents events) {
        removal.delete(author, UUID.randomUUID());

        assertThat(events.ofType(CommentDeleted.class)).isEmpty();
    }
}
