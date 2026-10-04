package com.piratetok.live;

import com.piratetok.live.auth.Ttwid;
import com.piratetok.live.connection.Wss;
import com.piratetok.live.fakes.FakeConnectProxy;
import com.piratetok.live.http.Api;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F6: ttwid, API and WSS all go through the configured proxy, credentials included. java.net.http has no
 * SOCKS support, so socks5:// must fail loudly instead of being misused as an HTTP proxy.
 */
class ProxyTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);
    private static final String BASIC = "Basic " + Base64.getEncoder().encodeToString("user:pw".getBytes(StandardCharsets.US_ASCII));

    private static String via(FakeConnectProxy p) {
        return "http://user:pw@127.0.0.1:" + p.port();
    }

    private static void refused(Executable call) {
        assertThrows(Exception.class, call); // the fake refuses every tunnel
    }

    private static void wss(String proxy) throws Exception {
        Wss.connectAsync("wss://webcast-ws.tiktok.com/webcast/im/ws_proxy/ws_reuse_supplement/?room_id=7",
            "abc", "7", Duration.ofSeconds(10), Duration.ofSeconds(5), "ua", null, "en-US,en;q=0.9", proxy,
            e -> {}, e -> {}, new AtomicBoolean(false), new CompletableFuture<>()).get(5, TimeUnit.SECONDS);
    }

    @Test
    void ttwidThroughConnectProxy() throws Exception {
        try (var p = new FakeConnectProxy()) {
            refused(() -> Ttwid.fetch(TIMEOUT, "ua", via(p)));
            assertEquals(List.of(new FakeConnectProxy.Hit("www.tiktok.com:443", BASIC)), p.hits);
        }
    }

    @Test
    void apiThroughConnectProxy() throws Exception {
        try (var p = new FakeConnectProxy()) {
            refused(() -> Api.checkOnline("someone", TIMEOUT, "en", "US", via(p)));
            assertEquals(List.of(new FakeConnectProxy.Hit("www.tiktok.com:443", BASIC)), p.hits);
        }
    }

    @Test
    void wssThroughConnectProxy() throws Exception {
        try (var p = new FakeConnectProxy()) {
            refused(() -> wss(via(p)));
            assertEquals(List.of(new FakeConnectProxy.Hit("webcast-ws.tiktok.com:443", BASIC)), p.hits);
        }
    }

    @Test
    void socksIsRejectedLoudly() {
        var ex = assertThrows(IllegalArgumentException.class, () -> Ttwid.fetch(TIMEOUT, "ua", "socks5://127.0.0.1:1080"));
        assertTrue(ex.getMessage().contains("no SOCKS"), ex.getMessage());
        assertThrows(IllegalArgumentException.class, () -> wss("socks5://127.0.0.1:1080"));
    }
}
