package me.imshy.chat.ws;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.LockSupport;
import me.imshy.chat.UserId;
import reactor.core.publisher.Sinks;

// A user's open connections, keyed by UserId — several per user (tab/device, ADR-0014);
// deliver() fans out to all of them (ConnectionRegistryTest).
class ConnectionRegistry {

    // How long a foreign thread waits out a concurrent-producer clash on a sink
    // before
    // giving up as undelivered (best-effort delivery, ADR-0014).
    // ConnectionRegistryConcurrencyTest
    // pins that a contended emit gives up inside this budget rather than spinning.
    private static final long EMIT_CONTENTION_BUDGET_NANOS = Duration.ofMillis(25).toNanos();
    private static final long EMIT_CONTENTION_PARK_NANOS = Duration.ofMillis(1).toNanos();

    private final ConcurrentHashMap<UserId, Set<Sinks.Many<OutboundEvent>>> connections = new ConcurrentHashMap<>();

    void connect(UserId user, Sinks.Many<OutboundEvent> outbound) {
        connections.compute(user, (ignored, existing) -> {
            Set<Sinks.Many<OutboundEvent>> outboundSinks = existing != null ? existing : ConcurrentHashMap.newKeySet();
            outboundSinks.add(outbound);
            return outboundSinks;
        });
    }

    void disconnect(UserId user, Sinks.Many<OutboundEvent> outbound) {
        connections.computeIfPresent(user, (ignored, outboundSinks) -> {
            outboundSinks.remove(outbound);
            return outboundSinks.isEmpty() ? null : outboundSinks;
        });
    }

    // Presence (ADR-0014): answered per user on request, never pushed as a feed.
    boolean isOnline(UserId user) {
        Set<Sinks.Many<OutboundEvent>> userConnections = connections.get(user);
        return userConnections != null && !userConnections.isEmpty();
    }

    boolean deliver(UserId sender, UserId recipient, String text) {
        Set<Sinks.Many<OutboundEvent>> recipientConnections = connections.get(recipient);
        if (recipientConnections == null)
            return false;

        DeliveredMessage message = DeliveredMessage.of(sender, text);
        boolean acceptedByAtLeastOne = false;
        for (Sinks.Many<OutboundEvent> outbound : recipientConnections) {
            acceptedByAtLeastOne |= emit(outbound, message);
        }
        return acceptedByAtLeastOne;
    }

    // The one write path to a connection's non-serialized sink. A producer clash is
    // parked
    // on, never spun (spinning burned the Netty event loop) and never thrown (that
    // tore the
    // sender's connection down) — ConnectionRegistryConcurrencyTest pins both.
    static boolean emit(Sinks.Many<OutboundEvent> outbound, OutboundEvent event) {
        long deadline = System.nanoTime() + EMIT_CONTENTION_BUDGET_NANOS;
        while (true) {
            Sinks.EmitResult result = outbound.tryEmitNext(event);
            if (result == Sinks.EmitResult.OK)
                return true;
            if (result == Sinks.EmitResult.FAIL_OVERFLOW) {
                terminate(outbound);
                return false;
            }
            if (result == Sinks.EmitResult.FAIL_NON_SERIALIZED && System.nanoTime() < deadline) {
                LockSupport.parkNanos(EMIT_CONTENTION_PARK_NANOS);
                continue;
            }
            return false;
        }
    }

    // Buffer full — the recipient stopped draining: end that connection rather than
    // grow the
    // heap (ChatWebSocketHandlerBufferTest). The retry keeps a concurrent emit from
    // swallowing it.
    private static void terminate(Sinks.Many<OutboundEvent> outbound) {
        long deadline = System.nanoTime() + EMIT_CONTENTION_BUDGET_NANOS;
        while (outbound.tryEmitError(new OutboundBufferOverflowException()) == Sinks.EmitResult.FAIL_NON_SERIALIZED
            && System.nanoTime() < deadline) {
            LockSupport.parkNanos(EMIT_CONTENTION_PARK_NANOS);
        }
    }
}
