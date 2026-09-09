package me.imshy.pictogram.social.internal;

import java.util.Optional;
import me.imshy.pictogram.post.PublishedPosts;
import me.imshy.pictogram.social.PostCommented;
import me.imshy.pictogram.social.PostLiked;
import me.imshy.pictogram.social.SocialEvent;
import me.imshy.pictogram.social.UserFollowed;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.modulith.events.EventExternalizationConfiguration;
import org.springframework.modulith.events.RoutingTarget;

/**
 * How {@code social}'s three {@code @Externalized} engagement events are shaped
 * for the {@code pictogram.social} Kafka topic (ADR-0015). The broker, the JPA
 * outbox and the relay are wired at the composition root; this — what the
 * payload looks like and which partition it lands on — is the producing
 * module's call, so it lives here.
 *
 * <p>
 * Each event maps to a {@link SocialEvent} keyed by the <em>recipient</em>: the
 * post author for a like or comment (resolved via
 * {@link PublishedPosts#authorOf}), the followed user for a follow. Keying by
 * recipient keeps one person's notifications on one partition, in order. A like
 * or comment on a post whose author cannot be resolved — a race with post
 * deletion — is dropped rather than externalised without a recipient.
 */
@Configuration(proxyBeanMethods = false)
class SocialEventExternalization {

    static final String TOPIC = "pictogram.social";

    // @Lazy because Modulith injects this into an EventListenerFactory that is
    // built before the application beans; the PublishedPosts proxy is only
    // dereferenced later, when an engagement event is actually published.
    @Bean
    EventExternalizationConfiguration pictogramSocialExternalization(@Lazy PublishedPosts publishedPosts) {
        var payloads = new SocialEventPayloads(publishedPosts);
        return EventExternalizationConfiguration.externalizing()
            .select(EventExternalizationConfiguration.annotatedAsExternalized().and(payloads::canBuildFrom))
            .mapping(payloads::from)
            .routeAll(
                payload -> RoutingTarget.forTarget(TOPIC).andKey(((SocialEvent) payload).recipientId().toString()))
            .routeMapped().build();
    }

    /**
     * The one place each engagement event is turned into its {@link SocialEvent}
     * wire form.
     */
    private record SocialEventPayloads(PublishedPosts publishedPosts) {

        Optional<SocialEvent> tryFrom(Object event) {
            return switch (event) {
                case PostLiked e -> publishedPosts.authorOf(e.postId())
                    .map(author -> SocialEvent.postLiked(author, e.viewer().asUserId(), e.postId(), e.likedAt()));
                case PostCommented e -> publishedPosts.authorOf(e.postId()).map(
                    author -> SocialEvent.postCommented(author, e.viewer().asUserId(), e.postId(), e.commentedAt()));
                case UserFollowed e ->
                    Optional.of(SocialEvent.userFollowed(e.followed(), e.follower(), e.followedAt()));
                default -> Optional.empty();
            };
        }

        boolean canBuildFrom(Object event) {
            return tryFrom(event).isPresent();
        }

        SocialEvent from(Object event) {
            return tryFrom(event).orElseThrow(
                () -> new IllegalStateException("Externalising an event with no resolvable recipient: " + event));
        }
    }
}
