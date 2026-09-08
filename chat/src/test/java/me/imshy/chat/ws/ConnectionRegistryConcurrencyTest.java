package me.imshy.chat.ws;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import me.imshy.chat.UserId;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;

// The registry is written concurrently: each connection runs on its own event-loop thread,
// so connect / disconnect / deliver for one user race each other. These pin the two spots
// where a naive read-then-mutate loses a connection or a message (#174).
class ConnectionRegistryConcurrencyTest {

    @Test
    void aSecondConnectionOpeningAsTheFirstClosesIsNeverOrphaned() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < 3_000; round++) {
                ConnectionRegistry registry = new ConnectionRegistry();
                UserId user = UserId.random();

                Sinks.Many<OutboundEvent> firstTab = Sinks.many().unicast().onBackpressureBuffer();
                firstTab.asFlux().subscribe(event -> {
                });
                registry.connect(user, firstTab);

                List<OutboundEvent> secondTabReceived = new CopyOnWriteArrayList<>();
                Sinks.Many<OutboundEvent> secondTab = Sinks.many().unicast().onBackpressureBuffer();
                secondTab.asFlux().subscribe(secondTabReceived::add);

                CyclicBarrier start = new CyclicBarrier(2);
                Future<?> opening = pool.submit(() -> {
                    awaitBarrier(start);
                    registry.connect(user, secondTab);
                });
                Future<?> closing = pool.submit(() -> {
                    awaitBarrier(start);
                    registry.disconnect(user, firstTab);
                });
                opening.get(5, TimeUnit.SECONDS);
                closing.get(5, TimeUnit.SECONDS);

                UserId anotherSender = UserId.random();
                boolean delivered = registry.deliver(anotherSender, user, "ping");

                assertThat(registry.isOnline(user))
                    .as("round %s: the second connection was orphaned by the racing disconnect", round).isTrue();
                assertThat(delivered)
                    .as("round %s: a message to the still-connected second tab was reported undelivered", round)
                    .isTrue();
                assertThat(secondTabReceived).as("round %s", round)
                    .containsExactly(DeliveredMessage.of(anotherSender, "ping"));
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void aContendedEmitGivesUpQuicklyRatherThanSpinningAndThrowingWhenAnotherThreadHoldsTheSink() throws Exception {
        Sinks.Many<OutboundEvent> sink = Sinks.many().unicast().onBackpressureBuffer();
        CountDownLatch consumerEntered = new CountDownLatch(1);
        CountDownLatch releaseConsumer = new CountDownLatch(1);
        sink.asFlux().subscribe(event -> {
            consumerEntered.countDown();
            await(releaseConsumer);
        });

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> holder = pool
                .submit(() -> ConnectionRegistry.emit(sink, OutboundSinkFixtures.sampleEvent()));
            assertThat(consumerEntered.await(5, TimeUnit.SECONDS)).isTrue();

            long startNanos = System.nanoTime();
            Future<Boolean> contender = pool
                .submit(() -> ConnectionRegistry.emit(sink, OutboundSinkFixtures.sampleEvent()));
            boolean contenderAccepted = contender.get(5, TimeUnit.SECONDS);
            Duration blocked = Duration.ofNanos(System.nanoTime() - startNanos);

            assertThat(contenderAccepted).as("a contended emit into a held sink must report not-accepted, not throw")
                .isFalse();
            assertThat(blocked).as("a contended emit must give up inside its small budget, not spin or park for ~1s")
                .isLessThan(Duration.ofMillis(300));

            releaseConsumer.countDown();
            holder.get(5, TimeUnit.SECONDS);
        } finally {
            releaseConsumer.countDown();
            pool.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }

    private static void awaitBarrier(CyclicBarrier barrier) {
        try {
            barrier.await(5, TimeUnit.SECONDS);
        } catch (Exception interrupted) {
            throw new IllegalStateException(interrupted);
        }
    }
}
