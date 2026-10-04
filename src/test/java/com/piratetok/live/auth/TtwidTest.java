package com.piratetok.live.auth;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Offline: a local responder omits the ttwid cookie N times, then sets it. */
class TtwidTest {

    private HttpServer server;
    private final AtomicInteger hits = new AtomicInteger();

    private URI serve(int missesBeforeCookie) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            int n = hits.incrementAndGet();
            exchange.getResponseHeaders().add("Set-Cookie", "tt_csrf_token=abc; Path=/");
            if (n > missesBeforeCookie) {
                exchange.getResponseHeaders().add("Set-Cookie", "ttwid=1%7Cfresh; Path=/; HttpOnly");
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");
    }

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private static String fetch(URI url) throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            return Ttwid.fetch(client, url, Duration.ofSeconds(5), "test-ua", Ttwid.FETCH_ATTEMPTS, Duration.ZERO);
        }
    }

    @Test
    void missingCookieThenCookieSucceeds() throws Exception {
        assertEquals("1%7Cfresh", fetch(serve(5)));
        assertEquals(6, hits.get());
    }

    @Test
    void firstResponseWithCookieDoesNotRetry() throws Exception {
        assertEquals("1%7Cfresh", fetch(serve(0)));
        assertEquals(1, hits.get());
    }

    @Test
    void neverACookieFailsAfterEightAttempts() throws Exception {
        URI url = serve(Integer.MAX_VALUE);
        var ex = assertThrows(IOException.class, () -> fetch(url));
        assertEquals(8, hits.get());
        assertTrue(ex.getMessage().contains("after 8 attempts"), ex.getMessage());
    }

    @Test
    void transportErrorPropagatesWithoutRetry() throws Exception {
        URI url = serve(0);
        server.stop(0);
        server = null;
        long start = System.nanoTime();
        try (var client = HttpClient.newHttpClient()) {
            assertThrows(IOException.class, () ->
                Ttwid.fetch(client, url, Duration.ofSeconds(2), "ua", Ttwid.FETCH_ATTEMPTS, Duration.ofSeconds(1)));
        }
        assertTrue(Duration.ofNanos(System.nanoTime() - start).compareTo(Duration.ofSeconds(1)) < 0,
            "transport error must not be retried");
    }

    @Test
    void defaultsMatchReference() {
        assertEquals(8, Ttwid.FETCH_ATTEMPTS);
        assertEquals(Duration.ofMillis(750), Ttwid.RETRY_DELAY);
    }
}
