package me.imshy.pictogram.scenario;

import java.net.URI;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import tools.jackson.databind.json.JsonMapper;

@Tag("blackbox")
class RunningStackAppTests
    implements
        CommentTests,
        PostDeleteTests,
        ProfileEditTests,
        FeedTests,
        FollowTests,
        FollowListTests,
        LikeTests,
        UserOnboardingTests,
        PostPublishTests,
        ViewProfileTests {

    private PictogramApp pictogramApp;

    @BeforeEach
    void connectToTheRunningStack() {
        URI baseUri = URI
            .create(Optional.ofNullable(System.getenv("PICTOGRAM_BASE_URL")).orElse("http://localhost:8080"));
        pictogramApp = new RunningStackApp(baseUri, JsonMapper.builder().build());
    }

    @Override
    public PictogramApp pictogram() {
        return pictogramApp;
    }
}
