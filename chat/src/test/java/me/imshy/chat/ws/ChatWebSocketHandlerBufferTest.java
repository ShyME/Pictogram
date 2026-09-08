package me.imshy.chat.ws;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Sinks;

// A half-open recipient socket (laptop sleeps, no FIN) never drains its outbound sink while
// other users keep sending to it. The per-connection buffer must be capped, and overflow
// must terminate that one connection rather than grow the heap until TCP keepalive or an
// OOM (#174).
class ChatWebSocketHandlerBufferTest {

    @Test
    void theOutboundBufferCapIsPinned() {
        assertThat(ChatWebSocketHandler.OUTBOUND_BUFFER_CAPACITY).isEqualTo(256);
    }

    @Test
    void theOutboundSinkIsBoundedRatherThanGrowingWithoutLimit() {
        Sinks.Many<OutboundEvent> outbound = ChatWebSocketHandler.outboundSink();
        outbound.asFlux().subscribe(OutboundSinkFixtures.demandNothing());

        Sinks.EmitResult result = Sinks.EmitResult.OK;
        int accepted = 0;
        for (; result == Sinks.EmitResult.OK
            && accepted < ChatWebSocketHandler.OUTBOUND_BUFFER_CAPACITY * 4; accepted++) {
            result = outbound.tryEmitNext(OutboundSinkFixtures.sampleEvent());
        }

        assertThat(result).isEqualTo(Sinks.EmitResult.FAIL_OVERFLOW);
        assertThat(accepted).isLessThanOrEqualTo(ChatWebSocketHandler.OUTBOUND_BUFFER_CAPACITY + 1);
    }

    @Test
    void overflowingTheBufferTerminatesTheConnectionsSink() {
        Sinks.Many<OutboundEvent> outbound = ChatWebSocketHandler.outboundSink();
        AtomicReference<Throwable> terminatedWith = new AtomicReference<>();
        BaseSubscriber<OutboundEvent> stalled = new BaseSubscriber<>() {
            @Override
            protected void hookOnSubscribe(Subscription subscription) {
                // request nothing: model a recipient socket that has stopped draining
            }

            @Override
            protected void hookOnError(Throwable throwable) {
                terminatedWith.set(throwable);
            }
        };
        outbound.asFlux().subscribe(stalled);

        boolean overflowed = false;
        for (int i = 0; !overflowed && i < ChatWebSocketHandler.OUTBOUND_BUFFER_CAPACITY * 4; i++) {
            overflowed = !ConnectionRegistry.emit(outbound, OutboundSinkFixtures.sampleEvent());
        }
        assertThat(overflowed).as("emit into a full bounded buffer reports not-accepted").isTrue();

        stalled.request(Long.MAX_VALUE);

        assertThat(terminatedWith.get()).isInstanceOf(OutboundBufferOverflowException.class);
    }

}
