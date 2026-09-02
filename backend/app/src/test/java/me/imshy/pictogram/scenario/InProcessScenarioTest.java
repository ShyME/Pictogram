package me.imshy.pictogram.scenario;

/**
 * Every {@code *Scenarios} mixin run through the {@link InProcessDriver} — {@code @Tag("fast")} via
 * {@link ScenarioTest}, every build. {@link BlackboxScenarioTest} is the same list against the
 * container. {@code OrphanMediaCollectionScenarioTest} stays separate (it needs in-process beans).
 */
class InProcessScenarioTest extends ScenarioTest
        implements DeletePostScenarios,
                EditProfileScenarios,
                FeedScenarios,
                FollowScenarios,
                FollowListScenarios,
                LikeScenarios,
                NewUserOnboardingScenarios,
                PublishPostScenarios,
                ViewProfileScenarios {}
