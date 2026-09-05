package me.imshy.pictogram.scenario;

/**
 * Every {@code *Tests} mixin run through the {@link SpringBootApp} —
 * {@code @Tag("fast")} via {@link AppIntegrationTest}, every build.
 * {@link RunningStackAppTests} is the same list against the running stack.
 * {@code OrphanMediaCollectionTest} stays separate (it needs in-process beans).
 */
class SpringBootAppTests extends AppIntegrationTest
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
}
