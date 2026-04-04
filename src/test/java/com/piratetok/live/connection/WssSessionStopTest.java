package com.piratetok.live.connection;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link Wss#connect} session-stop short-circuit and shared scheduler tolerance under burst.
 */
class WssSessionStopTest {

    @Test
    void connect_whenSessionStopAlreadyDone_returnsWithoutHandshake() {
        var stop = new AtomicBoolean(false);
        var sessionStop = new CompletableFuture<Void>();
        sessionStop.complete(null);

        assertDoesNotThrow(() -> Wss.connect(
                "wss://127.0.0.1:9/unused",
                "test-ttwid",
                "1",
                Duration.ofSeconds(30),
                "JUnit-WssSessionStopTest/1.0",
                null,
                e -> {},
                ex -> {},
                stop,
                sessionStop));
    }

    @Test
    void connect_manyConcurrentEarlyReturns_completeWithoutDeadlock() throws Exception {
        int n = 32;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            futures.add(pool.submit((Callable<Void>) () -> {
                var stop = new AtomicBoolean(false);
                var sessionStop = new CompletableFuture<Void>();
                sessionStop.complete(null);
                Wss.connect(
                        "wss://127.0.0.1:9/unused",
                        "t",
                        "1",
                        Duration.ofMillis(500),
                        "ua",
                        null,
                        e -> {},
                        ex -> {},
                        stop,
                        sessionStop);
                return null;
            }));
        }
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
    }
}
