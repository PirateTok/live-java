package com.piratetok.live;

import com.piratetok.live.events.EventType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Runs several {@link PirateTokClient} instances (one per username) for one minute, counts
 * {@link EventType#CHAT} per channel, then logs counts. Requires every username to be live.
 *
 * <p>Set {@code PIRATETOK_LIVE_TEST_USERS} to a comma-separated list (e.g. {@code user1,user2}).</p>
 */
@Tag("integration")
class PirateTokClientMultiUserChatLoadTest {

    private static final Logger log = Logger.getLogger(PirateTokClientMultiUserChatLoadTest.class.getName());

    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration STALE_TIMEOUT = Duration.ofSeconds(120);
    private static final Duration LIVE_WINDOW = Duration.ofMinutes(1);
    private static final Duration ALL_CONNECTED_WAIT = Duration.ofSeconds(120);
    private static final Duration SESSION_JOIN_WAIT = Duration.ofSeconds(120);

    @Test
    @EnabledIfEnvironmentVariable(named = "PIRATETOK_LIVE_TEST_USERS", matches = ".+")
    void multipleLiveClients_trackChatForOneMinute_thenLogCounts() throws Exception {
        List<String> users = parseUsers(System.getenv("PIRATETOK_LIVE_TEST_USERS"));
        assertFalse(users.isEmpty(), "PIRATETOK_LIVE_TEST_USERS contained no non-empty usernames");

        var allConnected = new CountDownLatch(users.size());
        var slots = new ArrayList<Slot>(users.size());
        var futures = new ArrayList<CompletableFuture<String>>(users.size());

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (String username : users) {
                var chatCount = new AtomicLong();
                var client = new PirateTokClient(username)
                        .cdnEU()
                        .timeout(HTTP_TIMEOUT)
                        .maxRetries(5)
                        .staleTimeout(STALE_TIMEOUT);
                client.on(EventType.CONNECTED, e -> allConnected.countDown());
                client.on(EventType.CHAT, e -> chatCount.incrementAndGet());
                slots.add(new Slot(username, client, chatCount));
                futures.add(client.connectAsync(executor));
            }

            if (!allConnected.await(ALL_CONNECTED_WAIT.toSeconds(), TimeUnit.SECONDS)) {
                for (Slot s : slots) {
                    s.client.disconnect();
                }
                try {
                    CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                            .get(SESSION_JOIN_WAIT.toSeconds(), TimeUnit.SECONDS);
                } catch (Exception ignored) {
                    // best-effort shutdown after failed connect phase
                }
                fail("not all clients reached CONNECTED within %ds (offline user, network, or rate limit)"
                        .formatted(ALL_CONNECTED_WAIT.toSeconds()));
            }

            Thread.sleep(LIVE_WINDOW);

            for (Slot s : slots) {
                s.client.disconnect();
            }

            try {
                CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                        .get(SESSION_JOIN_WAIT.toSeconds(), TimeUnit.SECONDS);
            } catch (Exception e) {
                fail("session futures did not finish after disconnect: " + e);
            }

            long total = 0;
            for (Slot s : slots) {
                long n = s.chatCount.get();
                total += n;
                log.info("multi-user chat load: username=" + s.username + " chatMessages=" + n);
            }
            log.info("multi-user chat load: totalChatMessages=" + total);
        }

        for (CompletableFuture<String> f : futures) {
            assertTrue(f.isDone() && !f.isCompletedExceptionally(), "expected clean session completion");
        }
    }

    private static List<String> parseUsers(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private record Slot(String username, PirateTokClient client, AtomicLong chatCount) {}
}
