package me.imshy.pictogram.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import me.imshy.pictogram.scenario.PictogramApi.Actor;
import me.imshy.pictogram.scenario.PictogramApi.Comment;
import me.imshy.pictogram.scenario.PictogramApi.CommentPage;
import me.imshy.pictogram.scenario.PictogramApi.DeleteOutcome;
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

    @Test
    default void aCommentAuthorDeletesTheirOwnCommentAndItLeavesTheThreadAndTheCount() {
        var ada = pictogram().registerViaGoogle("ada@example.com");
        ada.completeOnboarding("ada_cmtdel", "Ada", null);
        var bob = pictogram().registerViaGoogle("bob@example.com");
        bob.completeOnboarding("bob_cmtdel", "Bob", null);
        String post = bob.publishPost(bob.uploadPhoto(jpegPhoto()), "a photo").postId();

        Comment keep = bob.comment(post, "nice");
        Comment remove = ada.comment(post, "oops wrong post");
        assertThat(ada.commentCountsOf(post)).containsEntry(post, 2L);

        assertThat(ada.deleteComment(remove.commentId())).isEqualTo(DeleteOutcome.DELETED);

        assertThat(ada.commentsOn(post, null, null).comments())
                .extracting(Comment::commentId)
                .containsExactly(keep.commentId());
        assertThat(ada.commentCountsOf(post)).containsEntry(post, 1L);
    }

    @Test
    default void thePostAuthorCanRemoveAnyCommentButAnotherViewerCannot() {
        var ada = pictogram().registerViaGoogle("ada@example.com");
        ada.completeOnboarding("ada_cmtmod", "Ada", null);
        var bob = pictogram().registerViaGoogle("bob@example.com");
        bob.completeOnboarding("bob_cmtmod", "Bob", null);
        var cal = pictogram().registerViaGoogle("cal@example.com");
        cal.completeOnboarding("cal_cmtmod", "Cal", null);
        String post = bob.publishPost(bob.uploadPhoto(jpegPhoto()), "a photo").postId();

        Comment adasComment = ada.comment(post, "hi bob");

        assertThat(cal.deleteComment(adasComment.commentId())).isEqualTo(DeleteOutcome.FORBIDDEN);
        assertThat(bob.deleteComment(adasComment.commentId())).isEqualTo(DeleteOutcome.DELETED);
        assertThat(bob.commentsOn(post, null, null).comments()).isEmpty();
    }

    @Test
    default void deletingAPostRemovesItsCommentsFromTheThreadAndTheCount() {
        var ada = pictogram().registerViaGoogle("ada@example.com");
        ada.completeOnboarding("ada_cmtcascade", "Ada", null);
        var bob = pictogram().registerViaGoogle("bob@example.com");
        bob.completeOnboarding("bob_cmtcascade", "Bob", null);
        String post = bob.publishPost(bob.uploadPhoto(jpegPhoto()), "a photo").postId();
        ada.comment(post, "one");
        ada.comment(post, "two");

        assertThat(bob.deletePost(post)).isEqualTo(DeleteOutcome.DELETED);

        assertThat(ada.commentsOn(post, null, null).comments()).isEmpty();
        assertThat(ada.commentCountsOf(post)).containsEntry(post, 0L);
    }

    @Test
    default void theBatchCountReadReportsEveryRequestedPostIncludingOneWithNoComments() {
        var ada = pictogram().registerViaGoogle("ada@example.com");
        ada.completeOnboarding("ada_cmtcounts", "Ada", null);
        String chatty = ada.publishPost(ada.uploadPhoto(jpegPhoto()), "loud").postId();
        String quiet = ada.publishPost(ada.uploadPhoto(jpegPhoto()), "silent").postId();

        ada.comment(chatty, "a");
        ada.comment(chatty, "b");

        assertThat(ada.commentCountsOf(chatty, quiet)).containsEntry(chatty, 2L).containsEntry(quiet, 0L);
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
