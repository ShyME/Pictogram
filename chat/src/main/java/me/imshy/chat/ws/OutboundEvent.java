package me.imshy.chat.ws;

import me.imshy.chat.UserId;

// What a connection can receive: someone else's message, an explicit "undelivered"
// outcome for one this connection's own caller just sent (ADR-0014's Delivery language) —
// never both for the same send — or the answer to a presence query it asked.
sealed interface OutboundEvent permits DeliveredMessage, UndeliveredMessage, PresenceStatus {
}

record DeliveredMessage(String type, UserId senderUserId, String text) implements OutboundEvent {

    static DeliveredMessage of(UserId senderUserId, String text) {
        return new DeliveredMessage("message", senderUserId, text);
    }
}

record UndeliveredMessage(String type, UserId recipientUserId, String text) implements OutboundEvent {

    static UndeliveredMessage of(UserId recipientUserId, String text) {
        return new UndeliveredMessage("undelivered", recipientUserId, text);
    }
}

record PresenceStatus(String type, UserId userId, boolean online) implements OutboundEvent {

    static PresenceStatus of(UserId userId, boolean online) {
        return new PresenceStatus("presence", userId, online);
    }
}
