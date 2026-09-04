package me.imshy.pictogram.social.internal.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.Instant;
import me.imshy.pictogram.post.PostDeleted;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.social.internal.SocialModuleIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class CommentCleanupTest extends SocialModuleIntegrationTest {

    @Autowired
    Commenting commenting;

    @Autowired
    CommentThread thread;

    @Autowired
    ApplicationEventPublisher events;

    @MockitoBean
    Clock clock;

    @BeforeEach
    void bindClock() {
        given(clock.instant()).willReturn(Instant.parse("2026-09-04T12:00:00Z"));
    }

    @Test
    void deletingAPostHardDeletesItsThreadAndLeavesOtherThreadsAlone() {
        var deleted = PostId.random();
        var untouched = PostId.random();
        commenting.comment(ViewerId.random(), deleted, "on the doomed post");
        commenting.comment(ViewerId.random(), deleted, "also doomed");
        commenting.comment(ViewerId.random(), untouched, "still here");

        events.publishEvent(new PostDeleted(deleted, UserId.random(), MediaId.random(), Instant.now()));

        assertThat(thread.pageFor(deleted, null, null).comments()).isEmpty();
        assertThat(thread.pageFor(untouched, null, null).comments()).hasSize(1);
    }
}
