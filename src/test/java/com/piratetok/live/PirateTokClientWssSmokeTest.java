package com.piratetok.live;

import com.piratetok.live.events.EventType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Short WebSocket smoke tests against a real live room. Flaky under rate limits or quiet streams.
 *
 * <p>Requires {@code PIRATETOK_LIVE_TEST_USER} (live during the run). Uses EU CDN, modest retries.</p>
 *
 * <p>Test methods run concurrently (see {@code junit-platform.properties}, parallelism 8).</p>
 */
@Execution(ExecutionMode.CONCURRENT)
@Tag("integration")
class PirateTokClientWssSmokeTest {

    private static final Duration AWAIT_TRAFFIC = Duration.ofSeconds(90);
    private static final Duration AWAIT_CHAT = Duration.ofSeconds(120);
    private static final Duration AWAIT_GIFT = Duration.ofSeconds(180);
    private static final Duration AWAIT_LIKE = Duration.ofSeconds(120);
    private static final Duration AWAIT_JOIN = Duration.ofSeconds(150);
    private static final Duration AWAIT_FOLLOW = Duration.ofSeconds(180);
    private static final Duration AWAIT_SUBSCRIPTION = Duration.ofSeconds(240);

    @Test
    @EnabledIfEnvironmentVariable(named = "PIRATETOK_LIVE_TEST_USER", matches = ".+")
    void disconnect_unblocksConnectThreadAfterConnected() throws Exception {
        String user = System.getenv("PIRATETOK_LIVE_TEST_USER").strip();
        var connected = new CountDownLatch(1);
        var workerError = new AtomicReference<Throwable>();

        var client = new PirateTokClient(user)
                .cdnEU()
                .timeout(Duration.ofSeconds(15))
                .maxRetries(5)
                .staleTimeout(Duration.ofSeconds(45));
        client.on(EventType.CONNECTED, e -> connected.countDown());

        Thread worker = new Thread(() -> {
            try {
                client.connect();
            } catch (Throwable t) {
                workerError.set(t);
            }
        }, "wss-disconnect-" + user);
        worker.start();

        try {
            if (!connected.await(90, TimeUnit.SECONDS)) {
                fail("never reached CONNECTED within 90s (offline user or network)");
            }
            assertNull(workerError.get(), () -> "connect thread failed before disconnect: " + workerError.get());

            long t0 = System.currentTimeMillis();
            client.disconnect();
            worker.join(20_000);
            assertFalse(worker.isAlive(), "connect thread should exit after disconnect()");
            assertTrue(
                    System.currentTimeMillis() - t0 < 18_000,
                    "worker join should finish soon after disconnect (sessionStop + anyOf path)");
        } finally {
            client.disconnect();
            worker.join(Duration.ofSeconds(5).toMillis());
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "PIRATETOK_LIVE_TEST_USER", matches = ".+")
    void connect_receivesTrafficBeforeTimeout() throws Exception {
        String user = System.getenv("PIRATETOK_LIVE_TEST_USER").strip();
        awaitWssEvent(user, AWAIT_TRAFFIC, (client, hit) -> {
            client.on(EventType.ROOM_USER_SEQ, e -> hit.run());
            client.on(EventType.MEMBER, e -> hit.run());
            client.on(EventType.CHAT, e -> hit.run());
            client.on(EventType.LIKE, e -> hit.run());
            client.on(EventType.CONTROL, e -> hit.run());
        }, "no room traffic within %ds (quiet stream or block)".formatted(AWAIT_TRAFFIC.toSeconds()));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "PIRATETOK_LIVE_TEST_USER", matches = ".+")
    void connect_receivesChatBeforeTimeout() throws Exception {
        String user = System.getenv("PIRATETOK_LIVE_TEST_USER").strip();
        awaitWssEvent(user, AWAIT_CHAT, (client, hit) -> client.on(EventType.CHAT, e -> {
            @SuppressWarnings("unchecked")
            var chatter = (Map<String, Object>) e.data().getOrDefault("user", Map.of());
            System.out.println("[integration test chat] "
                    + chatter.getOrDefault("uniqueId", "?")
                    + ": "
                    + e.data().get("content"));
            hit.run();
        }), "no chat message within %ds (quiet stream or block)".formatted(AWAIT_CHAT.toSeconds()));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "PIRATETOK_LIVE_TEST_USER", matches = ".+")
    void connect_receivesGiftBeforeTimeout() throws Exception {
        String user = System.getenv("PIRATETOK_LIVE_TEST_USER").strip();
        awaitWssEvent(user, AWAIT_GIFT, (client, hit) -> client.on(EventType.GIFT, e -> {
            @SuppressWarnings("unchecked")
            var gifter = (Map<String, Object>) e.data().getOrDefault("user", Map.of());
            @SuppressWarnings("unchecked")
            var gift = (Map<String, Object>) e.data().getOrDefault("gift", Map.of());
            long diamonds = ((Number) gift.getOrDefault("diamondCount", 0)).longValue();
            long repeat = ((Number) e.data().getOrDefault("repeatCount", 1)).longValue();
            System.out.println("[integration test gift] "
                    + gifter.getOrDefault("uniqueId", "?")
                    + " -> "
                    + gift.getOrDefault("name", "?")
                    + " x"
                    + repeat
                    + " ("
                    + diamonds
                    + " diamonds each)");
            hit.run();
        }), "no gift within %ds (quiet stream or no gifts — try a busier stream)".formatted(AWAIT_GIFT.toSeconds()));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "PIRATETOK_LIVE_TEST_USER", matches = ".+")
    void connect_receivesLikeBeforeTimeout() throws Exception {
        String user = System.getenv("PIRATETOK_LIVE_TEST_USER").strip();
        awaitWssEvent(user, AWAIT_LIKE, (client, hit) -> client.on(EventType.LIKE, e -> {
            @SuppressWarnings("unchecked")
            var liker = (Map<String, Object>) e.data().getOrDefault("user", Map.of());
            System.out.println("[integration test like] "
                    + liker.getOrDefault("uniqueId", "?")
                    + " count="
                    + e.data().get("count")
                    + " total="
                    + e.data().get("total"));
            hit.run();
        }), "no like within %ds (quiet stream or block)".formatted(AWAIT_LIKE.toSeconds()));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "PIRATETOK_LIVE_TEST_USER", matches = ".+")
    void connect_receivesJoinBeforeTimeout() throws Exception {
        String user = System.getenv("PIRATETOK_LIVE_TEST_USER").strip();
        awaitWssEvent(user, AWAIT_JOIN, (client, hit) -> client.on(EventType.JOIN, e -> {
            @SuppressWarnings("unchecked")
            var member = (Map<String, Object>) e.data().getOrDefault("user", Map.of());
            System.out.println("[integration test join] " + member.getOrDefault("uniqueId", "?"));
            hit.run();
        }), "no join within %ds (try a busier stream)".formatted(AWAIT_JOIN.toSeconds()));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "PIRATETOK_LIVE_TEST_USER", matches = ".+")
    void connect_receivesFollowBeforeTimeout() throws Exception {
        String user = System.getenv("PIRATETOK_LIVE_TEST_USER").strip();
        awaitWssEvent(user, AWAIT_FOLLOW, (client, hit) -> client.on(EventType.FOLLOW, e -> {
            @SuppressWarnings("unchecked")
            var follower = (Map<String, Object>) e.data().getOrDefault("user", Map.of());
            System.out.println("[integration test follow] " + follower.getOrDefault("uniqueId", "?"));
            hit.run();
        }), "no follow within %ds (follows are infrequent — try a growing stream)".formatted(AWAIT_FOLLOW.toSeconds()));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "PIRATETOK_LIVE_TEST_USER", matches = ".+")
    @Disabled
    void connect_receivesSubscriptionSignalBeforeTimeout() throws Exception {
        String user = System.getenv("PIRATETOK_LIVE_TEST_USER").strip();
        awaitWssEvent(user, AWAIT_SUBSCRIPTION, (client, hit) -> {
            client.on(EventType.SUB_NOTIFY, e -> {
                System.out.println("[integration test subscription] subNotify");
                hit.run();
            });
            client.on(EventType.SUBSCRIPTION_NOTIFY, e -> {
                System.out.println("[integration test subscription] subscriptionNotify");
                hit.run();
            });
            client.on(EventType.SUB_CAPSULE, e -> {
                System.out.println("[integration test subscription] subCapsule");
                hit.run();
            });
            client.on(EventType.SUB_PIN_EVENT, e -> {
                System.out.println("[integration test subscription] subPinEvent");
                hit.run();
            });
        }, "no subscription-related event within %ds (need subs/gifts on a sub-enabled stream)".formatted(AWAIT_SUBSCRIPTION.toSeconds()));
    }

    private static void awaitWssEvent(
            String user,
            Duration await,
            BiConsumer<PirateTokClient, Runnable> registerListeners,
            String failureMessage
    ) throws Exception {
        var latch = new CountDownLatch(1);
        var workerError = new AtomicReference<Throwable>();
        Runnable onHit = latch::countDown;

        var client = new PirateTokClient(user)
                .cdnEU()
                .timeout(Duration.ofSeconds(15))
                .maxRetries(5)
                .staleTimeout(Duration.ofSeconds(45));

        registerListeners.accept(client, onHit);

        Thread worker = new Thread(() -> {
            try {
                client.connect();
            } catch (Throwable t) {
                workerError.set(t);
            }
        }, "wss-smoke-" + user);
        worker.start();

        try {
            boolean got = latch.await(await.toSeconds(), TimeUnit.SECONDS);
            assertNull(workerError.get(), () -> "connect thread failed: " + workerError.get());
            assertTrue(got, failureMessage);
        } finally {
            client.disconnect();
            worker.join(Duration.ofSeconds(30).toMillis());
            assertFalse(worker.isAlive(), "worker should exit after disconnect");
        }
    }
}
