package me.imshy.chat.ws;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import me.imshy.chat.UserId;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;

class ConnectionRegistryTest {

    private final ConnectionRegistry registry = new ConnectionRegistry();

    @Test
    void deliversToEveryConnectionTheRecipientHasOpen() {
        UserId sender = UserId.random();
        UserId recipient = UserId.random();
        List<OutboundEvent> firstTab = record(recipient);
        List<OutboundEvent> secondTab = record(recipient);

        boolean delivered = registry.deliver(sender, recipient, "hello");

        assertThat(delivered).isTrue();
        assertThat(firstTab).containsExactly(DeliveredMessage.of(sender, "hello"));
        assertThat(secondTab).containsExactly(DeliveredMessage.of(sender, "hello"));
    }

    @Test
    void reportsNoDeliveryForAUserWithNoOpenConnection() {
        boolean delivered = registry.deliver(UserId.random(), UserId.random(), "hello");

        assertThat(delivered).isFalse();
    }

    @Test
    void aDisconnectedConnectionNoLongerReceivesMessages() {
        UserId sender = UserId.random();
        UserId recipient = UserId.random();
        Sinks.Many<OutboundEvent> outbound = Sinks.many().unicast().onBackpressureBuffer();
        registry.connect(recipient, outbound);
        registry.disconnect(recipient, outbound);

        boolean delivered = registry.deliver(sender, recipient, "hello");

        assertThat(delivered).isFalse();
    }

    private List<OutboundEvent> record(UserId user) {
        List<OutboundEvent> received = new ArrayList<>();
        Sinks.Many<OutboundEvent> outbound = Sinks.many().unicast().onBackpressureBuffer();
        outbound.asFlux().subscribe(received::add);
        registry.connect(user, outbound);
        return received;
    }
}
