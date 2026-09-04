package me.imshy.pictogram.social.internal.comment;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.social.CommentCounts.PostComments;
import me.imshy.pictogram.social.internal.SocialModuleIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CommentTallyTest extends SocialModuleIntegrationTest {

    @Autowired
    Commenting commenting;

    @Autowired
    CommentTally tally;

    @Test
    void reportsEachRequestedPostsCommentCount() {
        var chatty = PostId.random();
        var quiet = PostId.random();

        commenting.comment(ViewerId.random(), chatty, "one");
        commenting.comment(ViewerId.random(), chatty, "two");
        commenting.comment(ViewerId.random(), quiet, "only me");

        Map<PostId, PostComments> byId = index(tally.of(List.of(chatty, quiet)));

        assertThat(byId.get(chatty)).isEqualTo(new PostComments(chatty, 2));
        assertThat(byId.get(quiet)).isEqualTo(new PostComments(quiet, 1));
    }

    @Test
    void returnsARecordForEveryRequestedIdIncludingOneWithNoComments() {
        var untouched = PostId.random();

        assertThat(tally.of(List.of(untouched))).containsExactly(new PostComments(untouched, 0));
    }

    @Test
    void anEmptyRequestReturnsNothing() {
        assertThat(tally.of(List.of())).isEmpty();
    }

    private static Map<PostId, PostComments> index(List<PostComments> rows) {
        return rows.stream().collect(toMap(PostComments::post, identity()));
    }
}
