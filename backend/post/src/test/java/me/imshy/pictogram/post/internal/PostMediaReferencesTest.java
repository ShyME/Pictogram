package me.imshy.pictogram.post.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;
import me.imshy.pictogram.media.PostReferences;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PostMediaReferencesTest extends PostModuleIntegrationTest {

    @Autowired
    Publishing publishing;

    @Autowired
    PostDeletion deletion;

    @Autowired
    PostReferences postReferences;

    @Test
    void reportsOnlyTheMediaThatAPublishedPostStillReferences() {
        var author = UserId.random();
        var referenced = MediaId.random();
        var afterDelete = MediaId.random();
        var neverPosted = MediaId.random();
        given(media.ownerOf(referenced)).willReturn(Optional.of(author));
        given(media.ownerOf(afterDelete)).willReturn(Optional.of(author));
        publishing.publish(author, referenced, null);
        var doomed = publishing.publish(author, afterDelete, null);
        deletion.delete(author, doomed.postId());

        var stillReferenced = postReferences.referencedAmong(List.of(referenced, afterDelete, neverPosted));

        assertThat(stillReferenced).containsExactly(referenced);
    }

    @Test
    void reportsNothingForAnEmptyOrAllUnknownCandidateSet() {
        assertThat(postReferences.referencedAmong(List.of())).isEmpty();
        assertThat(postReferences.referencedAmong(List.of(MediaId.random()))).isEmpty();
    }
}
