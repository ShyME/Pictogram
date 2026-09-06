package me.imshy.chat.ws;

import me.imshy.chat.UserId;

// The two things a connection can receive: someone else's message, or an explicit
// "undelivered" outcome for one this connection's own caller just sent (ADR-0014's
// Delivery language) — never both for the same send.
sealed interface OutboundEvent permits DeliveredMessage, UndeliveredMessage {
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
