package me.imshy.pictogram.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import me.imshy.pictogram.scenario.PictogramApp.FeedPage;
import me.imshy.pictogram.scenario.PictogramApp.PictogramApi;
import me.imshy.pictogram.scenario.PictogramApp.Post;
import org.junit.jupiter.api.Test;

interface FeedTests extends AppUnderTest {

    @Test
    default void aViewerFollowsTwoPeopleAndSeesTheirInterleavedPostsNewestFirstPagingWithoutGaps() {
        var ada = pictogram().registerViaGoogle("ada@example.com");
        ada.completeOnboarding("ada_feed", "Ada", null);
        var bob = pictogram().registerViaGoogle("bob@example.com");
        String bobId = bob.completeOnboarding("bob_feed", "Bob", null).userId();
        var carol = pictogram().registerViaGoogle("carol@example.com");
        String carolId = carol.completeOnboarding("carol_feed", "Carol", null).userId();

        ada.follow(bobId);
        ada.follow(carolId);

        List<String> publishedOldestFirst = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            publishedOldestFirst.add(bob.publishPost(bob.uploadPhoto(JpegPhoto.some()), "bob " + i).postId());
            publishedOldestFirst.add(carol.publishPost(carol.uploadPhoto(JpegPhoto.some()), "carol " + i).postId());
        }
        List<String> newestFirst = publishedOldestFirst.reversed();

        assertThat(ada.openFeed().postIds()).isEqualTo(newestFirst);
        assertThat(drainFeed(ada, 3)).isEqualTo(newestFirst);
    }

    @Test
    default void aPostPublishedBetweenPageFetchesCausesNoDuplicatesOrSkips() {
        var ada = pictogram().registerViaGoogle("ada@example.com");
        ada.completeOnboarding("ada_keyset", "Ada", null);
        var bob = pictogram().registerViaGoogle("bob@example.com");
        String bobId = bob.completeOnboarding("bob_keyset", "Bob", null).userId();
        ada.follow(bobId);

        List<String> firstBatch = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            firstBatch.add(bob.publishPost(bob.uploadPhoto(JpegPhoto.some()), "old " + i).postId());
        }

        FeedPage firstPage = ada.openFeed(null, 2);
        assertThat(firstPage.postIds()).containsExactly(firstBatch.get(3), firstBatch.get(2));

        String published = bob.publishPost(bob.uploadPhoto(JpegPhoto.some()), "brand new").postId();

        List<String> rest = new ArrayList<>();
        String cursor = firstPage.nextCursor();
        while (cursor != null) {
            FeedPage page = ada.openFeed(cursor, 2);
            rest.addAll(page.postIds());
            cursor = page.nextCursor();
        }

        assertThat(rest).containsExactly(firstBatch.get(1), firstBatch.get(0));
        assertThat(rest).doesNotContain(published);
    }

    @Test
    default void followingSomeoneRevealsTheirExistingPostsAndUnfollowingHidesThem() {
        var ada = pictogram().registerViaGoogle("ada@example.com");
        ada.completeOnboarding("ada_toggle", "Ada", null);
        var bob = pictogram().registerViaGoogle("bob@example.com");
        String bobId = bob.completeOnboarding("bob_toggle", "Bob", null).userId();

        bob.publishPost(bob.uploadPhoto(JpegPhoto.some()), "posted before ada followed");
        Post onBobsGrid = bob.postsOf(bobId).getFirst();

        assertThat(ada.openFeed().isEmpty()).isTrue();

        ada.follow(bobId);
        assertThat(ada.openFeed().posts()).singleElement().isEqualTo(onBobsGrid);

        ada.unfollow(bobId);
        assertThat(ada.openFeed().isEmpty()).isTrue();
    }

    private static List<String> drainFeed(PictogramApi viewer, int pageSize) {
        List<String> ids = new ArrayList<>();
        String cursor = null;
        do {
            FeedPage page = viewer.openFeed(cursor, pageSize);
            ids.addAll(page.postIds());
            cursor = page.nextCursor();
        } while (cursor != null);
        return ids;
    }
}
