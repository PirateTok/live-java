package com.piratetok.live.connection;

import com.piratetok.live.events.EventType;
import com.piratetok.live.events.TikTokEvent;
import com.piratetok.live.fakes.FakeWebcast;
import com.piratetok.live.proto.Proto;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** F2 + F7 + F8 against a fake webcast server: frames, ack and what goes on the wire. */
class WireTest {

    // non-UTF-8 on purpose: internal_ext is opaque bytes and must be echoed verbatim
    private static final byte[] EXT = {(byte) 0xff, 0x00, (byte) 0x80, 0x7a, (byte) 0xc3};

    private static Map<String, String> query(String target) {
        var q = new HashMap<String, String>();
        for (String kv : URI.create("http://x" + target).getRawQuery().split("&")) {
            int eq = kv.indexOf('=');
            q.put(kv.substring(0, eq), URLDecoder.decode(kv.substring(eq + 1), StandardCharsets.UTF_8));
        }
        return q;
    }

    @Test
    void sessionAgainstFakeWebcast() throws Exception {
        try (var server = new FakeWebcast()) {
            String url = WssUrl.build("HOST", "7", "ro", "RO", false, Duration.ofSeconds(2))
                .replace("wss://HOST", "ws://127.0.0.1:" + server.port());
            List<TikTokEvent> events = new CopyOnWriteArrayList<>();
            CompletableFuture<Void> session = Wss.connectAsync(url, "abc", "7", Duration.ofSeconds(2),
                Duration.ofSeconds(5), "UA-test/1.0", "sessionid=s1", "ro-RO,ro;q=0.9", null,
                events::add, e -> {}, new AtomicBoolean(false), new CompletableFuture<>());

            server.accept();
            List<Proto.ProtoMap> frames = new ArrayList<>();
            frames.add(Proto.decode(server.receiveBinary()));
            frames.add(Proto.decode(server.receiveBinary()));

            byte[] chat = Proto.encode(w -> w.writeString(3, "hello from fake"));
            byte[] message = Proto.encode(w -> {
                w.writeString(1, "WebcastChatMessage");
                w.writeBytes(2, chat);
            });
            byte[] response = Proto.encode(w -> {
                w.writeMessage(1, message);
                w.writeBytes(5, EXT);
                w.writeBool(9, true);
            });
            server.sendBinary(Proto.encode(w -> {
                w.writeInt64(2, 4242);
                w.writeString(7, "msg");
                w.writeBytes(8, response);
            }));

            Proto.ProtoMap ack;
            do {
                ack = Proto.decode(server.receiveBinary());
            } while (!"ack".equals(ack.getString(7)));
            server.sendClose();
            session.get(5, TimeUnit.SECONDS);

            assertEquals(List.of("hb", "im_enter_room"), frames.stream().map(f -> f.getString(7)).toList());
            assertEquals(4242L, ack.getVarint(2));
            assertArrayEquals(EXT, ack.getRawBytes(8));

            assertEquals("UA-test/1.0", server.head.get("user-agent"));
            assertEquals("ttwid=abc; sessionid=s1", server.head.get("cookie"));
            assertEquals("ro-RO,ro;q=0.9", server.head.get("accept-language"));
            assertEquals("https://www.tiktok.com", server.head.get("origin"));
            Map<String, String> q = query(server.head.get("request-target"));
            assertEquals("7", q.get("room_id"));
            assertEquals("ro", q.get("webcast_language"));
            assertEquals("ro", q.get("app_language"));
            assertEquals("ro-RO", q.get("browser_language"));
            assertEquals("", q.get("compress"));
            assertEquals("2000", q.get("heartbeat_duration"));

            var chats = events.stream().filter(e -> EventType.CHAT.equals(e.type())).toList();
            assertEquals(1, chats.size());
            assertEquals("hello from fake", chats.getFirst().data().get("content"));
        }
    }
}
