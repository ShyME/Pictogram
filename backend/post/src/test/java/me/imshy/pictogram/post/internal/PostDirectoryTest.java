package me.imshy.pictogram.post.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PostDirectoryTest extends PostModuleIntegrationTest {

    @Autowired
    PostDirectory directory;

    @Autowired
    PostPublishing postPublishing;

    @Test
    void batchLookupReturnsAViewPerKnownIdAndOmitsTheRest() {
        var author = UserId.random();
        var first = publish(author, "first");
        var second = publish(author, "second");
        var missing = PostId.random();

        var views = directory.byIds(List.of(first, missing, second));

        assertThat(views).extracting(PostView::postId).containsExactlyInAnyOrder(first, second);
        assertThat(views).extracting(PostView::caption).containsExactlyInAnyOrder("first", "second");
    }

    @Test
    void eachViewCarriesTheAuthorAndMediaSoTheRowCanLinkAndShowAThumbnail() {
        var author = UserId.random();
        var mediaId = MediaId.random();
        given(mediaCatalog.ownerOf(mediaId)).willReturn(Optional.of(author));
        var postId = postPublishing.publish(author, mediaId, "with media").postId();

        var view = directory.byIds(List.of(postId)).getFirst();

        assertThat(view.postId()).isEqualTo(postId);
        assertThat(view.authorId()).isEqualTo(author);
        assertThat(view.mediaId()).isEqualTo(mediaId);
    }

    @Test
    void batchLookupOfNothingIsEmpty() {
        assertThat(directory.byIds(List.of())).isEmpty();
    }

    private PostId publish(UserId author, String caption) {
        var mediaId = MediaId.random();
        given(mediaCatalog.ownerOf(mediaId)).willReturn(Optional.of(author));
        return postPublishing.publish(author, mediaId, caption).postId();
    }
}
