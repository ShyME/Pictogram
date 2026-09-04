package me.imshy.pictogram.scenario;

import java.net.URI;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import tools.jackson.databind.json.JsonMapper;

/**
 * Every {@code *Scenarios} mixin run through the {@link ContainerDriver} — the built application
 * image over HTTP, the truth of what production does. Same list as {@link InProcessScenarioTest}; a
 * divergence between the two is a bug in one transport. {@code @Tag("blackbox")} keeps it to the CI
 * {@code main} job and {@code ./gradlew build -PincludeBlackbox}.
 */
@Tag("blackbox")
class BlackboxScenarioTest
        implements CommentScenarios,
                DeletePostScenarios,
                EditProfileScenarios,
                FeedScenarios,
                FollowScenarios,
                FollowListScenarios,
                LikeScenarios,
                NewUserOnboardingScenarios,
                PublishPostScenarios,
                ViewProfileScenarios {

    private PictogramApi pictogram;

    @BeforeEach
    void connectToTheRunningStack() {
        URI baseUri = URI.create(
                Optional.ofNullable(System.getenv("PICTOGRAM_BASE_URL")).orElse("http://localhost:8080"));
        pictogram = new ContainerDriver(baseUri, JsonMapper.builder().build());
    }

    @Override
    public PictogramApi pictogram() {
        return pictogram;
    }
}
