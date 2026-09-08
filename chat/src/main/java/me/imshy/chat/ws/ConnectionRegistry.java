package me.imshy.chat.ws;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import me.imshy.chat.UserId;
import reactor.core.publisher.Sinks;

// Every open connection a user currently has, keyed by UserId — a caller may hold several
// (one per tab/device, ADR-0014), and a message to that user fans out to all of them.
class ConnectionRegistry {

    // A fresh handler per call, never cached: busyLooping's deadline is fixed at
    // construction (its own javadoc warns against reuse), so a shared instance
    // would
    // silently stop retrying for good soon after the process starts.
    // Package-visible —
    // every producer onto a connection's outbound sink uses this, including
    // ChatWebSocketHandler's own undelivered-outcome emission, since that races
    // this
    // one on the same sink.
    static Sinks.EmitFailureHandler emitRetrying() {
        return Sinks.EmitFailureHandler.busyLooping(Duration.ofSeconds(1));
    }

    private final ConcurrentHashMap<UserId, Set<Sinks.Many<OutboundEvent>>> connections = new ConcurrentHashMap<>();

    void connect(UserId user, Sinks.Many<OutboundEvent> outbound) {
        connections.computeIfAbsent(user, ignored -> ConcurrentHashMap.newKeySet()).add(outbound);
    }

    // Presence (ADR-0014): whether the user has at least one connection open right
    // now —
    // answered per user, on request, never pushed as a feed.
    boolean isOnline(UserId user) {
        Set<Sinks.Many<OutboundEvent>> userConnections = connections.get(user);
        return userConnections != null && !userConnections.isEmpty();
    }

    void disconnect(UserId user, Sinks.Many<OutboundEvent> outbound) {
        connections.computeIfPresent(user, (ignored, outboundSinks) -> {
            outboundSinks.remove(outbound);
            return outboundSinks.isEmpty() ? null : outboundSinks;
        });
    }

    // Fans a message out to every connection the recipient currently holds; reports
    // whether there was at least one to deliver to.
    boolean deliver(UserId sender, UserId recipient, String text) {
        Set<Sinks.Many<OutboundEvent>> recipientConnections = connections.get(recipient);
        if (recipientConnections == null || recipientConnections.isEmpty())
            return false;

        DeliveredMessage message = DeliveredMessage.of(sender, text);
        for (Sinks.Many<OutboundEvent> outbound : recipientConnections) {
            outbound.emitNext(message, emitRetrying());
        }
        return true;
    }
}
