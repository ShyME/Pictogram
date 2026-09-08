package me.imshy.chat.ws;

import me.imshy.chat.UserId;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;

// Shared setup for the tests that exercise a connection's outbound sink directly.
final class OutboundSinkFixtures {

    private OutboundSinkFixtures() {
    }

    static OutboundEvent sampleEvent() {
        return DeliveredMessage.of(UserId.random(), "m");
    }

    // A subscriber that never requests: models a recipient socket that has stopped
    // draining.
    static BaseSubscriber<OutboundEvent> demandNothing() {
        return new BaseSubscriber<>() {
            @Override
            protected void hookOnSubscribe(Subscription subscription) {
            }
        };
    }
}
