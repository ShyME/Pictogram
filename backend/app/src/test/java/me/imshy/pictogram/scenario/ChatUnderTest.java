package me.imshy.pictogram.scenario;

import java.net.URI;

interface ChatUnderTest {

    // The fixed subprotocol the browser offers alongside the token; the server
    // echoes
    // it back (a handshake echoing nothing is dropped). Mirrors chatConnection.ts.
    String CHAT_SUBPROTOCOL = "pictogram-chat";

    URI chatBaseUri();

}
