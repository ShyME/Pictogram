package me.imshy.pictogram.post.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import me.imshy.pictogram.post.PostStats;
import me.imshy.pictogram.post.internal.PostDeletion;
import me.imshy.pictogram.post.internal.PostModuleIntegrationTest;
import me.imshy.pictogram.post.internal.PostPublishing;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

// The consumer-contract test for the published PostStats interface (#233): the product-stat
// gauge job in :app is its one caller.
class PostStatsContractTest extends PostModuleIntegrationTest {

    @Autowired
    PostPublishing postPublishing;

    @Autowired
    PostDeletion postDeletion;

    @Autowired
    PostStats postStats;

    @Test
    void totalCountsEveryPostAcrossAuthors() {
        var ada = UserId.random();
        var bob = UserId.random();

        publish(ada);
        publish(bob);
        publish(ada);

        assertThat(postStats.total()).isEqualTo(3);
    }

    @Test
    void totalDropsWhenAPostIsDeleted() {
        var ada = UserId.random();
        var post = publish(ada);
        publish(ada);

        postDeletion.delete(ada, post);

        assertThat(postStats.total()).isEqualTo(1);
    }

    private PostId publish(UserId author) {
        var mediaId = MediaId.random();
        given(mediaCatalog.ownerOf(mediaId)).willReturn(Optional.of(author));
        return postPublishing.publish(author, mediaId, null).postId();
    }
}
