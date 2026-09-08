package me.imshy.pictogram.social.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.social.CommentCounts;
import me.imshy.pictogram.social.CommentCounts.PostComments;
import me.imshy.pictogram.social.internal.SocialModuleIntegrationTest;
import me.imshy.pictogram.social.internal.comment.CommentThread;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CommentCountsContractTest extends SocialModuleIntegrationTest {

    @Autowired
    CommentThread thread;

    @Autowired
    CommentCounts commentCounts;

    @Test
    void returnsExactlyOneRowPerRequestedPost() {
        var chatty = PostId.random();
        var quiet = PostId.random();
        var untouched = PostId.random();
        thread.comment(ViewerId.random(), chatty, "one");
        thread.comment(ViewerId.random(), chatty, "two");
        thread.comment(ViewerId.random(), quiet, "only me");

        var rows = commentCounts.of(List.of(chatty, quiet, untouched));

        assertThat(rows).extracting(PostComments::post).containsExactlyInAnyOrder(chatty, quiet, untouched);
        assertThat(rows).contains(new PostComments(chatty, 2), new PostComments(quiet, 1));
    }

    @Test
    void anUnknownPostReadsAsTheZeroValue() {
        var unknown = PostId.random();

        assertThat(commentCounts.of(List.of(unknown))).containsExactly(new PostComments(unknown, 0));
    }

    @Test
    void anEmptyRequestReturnsNoRows() {
        assertThat(commentCounts.of(List.of())).isEmpty();
    }
}
