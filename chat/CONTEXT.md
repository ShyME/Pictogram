# Chat

Live 1:1 messaging between two users. Unlike every other context here, `chat` is not a
module of the Spring Boot deployable — it is a separate service, coupled to the rest of
the system only by verifying who a caller is (ADR-0014).

## Language

**Message**:
A piece of text one user sends to exactly one other. Delivered live or not at all — never
stored, never retried.
_Avoid_: DM, chat (as the noun for one message), note

**Sender** / **Recipient**:
The two roles in one message: who sent it, who it was sent to.
_Avoid_: Author, target, participant

**Conversation**:
The live exchange of messages between two users. It exists only while messages are
actively passing between them — there is no persisted, addressable conversation to
return to in v1.
_Avoid_: Thread, DM thread, chat room, channel

**Delivery**:
Whether a message reached its recipient. An undelivered message is reported to the
sender immediately; it is not queued or retried for later delivery.
_Avoid_: Send status, receipt, read receipt

**Presence**:
Whether a user is currently reachable for live delivery. Reported per user, on request —
not pushed as a feed of everyone's status.
_Avoid_: Online status, availability, last seen
