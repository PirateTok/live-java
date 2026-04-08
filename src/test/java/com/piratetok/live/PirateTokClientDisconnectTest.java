package com.piratetok.live;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PirateTokClient#disconnect()} completes the active WSS session future and sets stop.
 */
class PirateTokClientDisconnectTest {

    @Test
    void disconnect_withoutConnect_setsStopOnly() throws Exception {
        var client = new PirateTokClient("any_user");
        assertFalse(reflectStop(client).get());

        assertDoesNotThrow(client::disconnect);

        assertTrue(reflectStop(client).get());
    }

    @Test
    void disconnect_completesActiveSessionStopFuture() throws Exception {
        var client = new PirateTokClient("any_user");
        var sessionStop = new CompletableFuture<Void>();
        reflectActiveSessionStop(client).set(sessionStop);

        assertFalse(sessionStop.isDone());
        client.disconnect();

        assertTrue(reflectStop(client).get());
        assertTrue(sessionStop.isDone());
    }

    @Test
    void disconnect_idempotent_secondCallDoesNotThrow() throws Exception {
        var client = new PirateTokClient("any_user");
        client.disconnect();
        assertDoesNotThrow(client::disconnect);
        assertTrue(reflectStop(client).get());
    }

    @SuppressWarnings("unchecked")
    private static AtomicReference<CompletableFuture<Void>> reflectActiveSessionStop(PirateTokClient client)
            throws Exception {
        Field f = PirateTokClient.class.getDeclaredField("activeSessionStop");
        f.setAccessible(true);
        return (AtomicReference<CompletableFuture<Void>>) f.get(client);
    }

    private static AtomicBoolean reflectStop(PirateTokClient client) throws Exception {
        Field f = PirateTokClient.class.getDeclaredField("stop");
        f.setAccessible(true);
        return (AtomicBoolean) f.get(client);
    }
}
