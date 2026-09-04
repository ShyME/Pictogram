package me.imshy.pictogram.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import me.imshy.pictogram.scenario.PictogramApi.Actor;
import me.imshy.pictogram.scenario.PictogramApi.Comment;
import me.imshy.pictogram.scenario.PictogramApi.CommentPage;
import org.junit.jupiter.api.Test;

interface CommentScenarios extends PictogramScenario {

    @Test
    default void aViewerCommentsOnAPostAndSeesItInTheThreadOldestFirst() {
        var ada = pictogram().registerViaGoogle("ada@example.com");
        ada.completeOnboarding("ada_comments", "Ada", null);
        var bob = pictogram().registerViaGoogle("bob@example.com");
        bob.completeOnboarding("bob_comments", "Bob", null);
        String post = bob.publishPost(bob.uploadPhoto(jpegPhoto()), "a photo").postId();

        Comment first = ada.comment(post, "great light");
        Comment second = bob.comment(post, "thanks!");

        List<Comment> thread = ada.commentsOn(post, null, null).comments();
        assertThat(thread.stream().map(Comment::commentId)).containsExactly(first.commentId(), second.commentId());
        assertThat(thread.get(0).body()).isEqualTo("great light");
        assertThat(thread.get(0).authorId()).isEqualTo(first.authorId());
    }

    @Test
    default void theThreadPagesForwardAcrossCursorsWithNoDuplicatesOrSkips() {
        var ada = pictogram().registerViaGoogle("ada@example.com");
        ada.completeOnboarding("ada_thread", "Ada", null);
        var bob = pictogram().registerViaGoogle("bob@example.com");
        bob.completeOnboarding("bob_thread", "Bob", null);
        String post = bob.publishPost(bob.uploadPhoto(jpegPhoto()), "a photo").postId();

        List<String> written = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            written.add(ada.comment(post, "comment " + i).commentId());
        }

        List<String> whole = bob.commentsOn(post, null, 50).comments().stream()
                .map(Comment::commentId)
                .toList();
        List<String> paged = drainThread(bob, post, 2);

        assertThat(whole).containsExactlyInAnyOrderElementsOf(written);
        assertThat(paged).doesNotHaveDuplicates().containsExactlyElementsOf(whole);
    }

    @Test
    default void aViewerMayCommentOnTheirOwnPost() {
        var ada = pictogram().registerViaGoogle("ada@example.com");
        ada.completeOnboarding("ada_selfcmt", "Ada", null);
        String own = ada.publishPost(ada.uploadPhoto(jpegPhoto()), "mine").postId();

        ada.comment(own, "first");

        assertThat(ada.commentsOn(own, null, null).comments())
                .singleElement()
                .satisfies(comment -> assertThat(comment.body()).isEqualTo("first"));
    }

    private static List<String> drainThread(Actor viewer, String postId, int pageSize) {
        List<String> ids = new ArrayList<>();
        String cursor = null;
        do {
            CommentPage page = viewer.commentsOn(postId, cursor, pageSize);
            assertThat(page.comments()).hasSizeLessThanOrEqualTo(pageSize);
            page.comments().forEach(comment -> ids.add(comment.commentId()));
            cursor = page.nextCursor();
        } while (cursor != null);
        return ids;
    }
}
