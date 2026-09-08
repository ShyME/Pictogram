package me.imshy.chat.ws;

// Terminates a connection whose outbound buffer filled up because the recipient socket
// stopped draining it (#174) — best-effort delivery (ADR-0014) does not queue past the cap.
class OutboundBufferOverflowException extends RuntimeException {

    OutboundBufferOverflowException() {
        super("outbound buffer overflowed; terminating the connection");
    }
}
