package com.piratetok.live.connection;

import org.junit.jupiter.api.Test;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WssUrlTest {

    @Test
    void build_schemeHostPathAndRoomId() {
        String cdn = "webcast-ws.eu.tiktok.com";
        String roomId = "7123456789012345678";
        String url = WssUrl.build(cdn, roomId, "en", "US");

        assertTrue(url.startsWith("wss://" + cdn + "/webcast/im/ws_proxy/ws_reuse_supplement/?"));

        Map<String, String> q = parseQuery(url.substring(url.indexOf('?') + 1));
        assertEquals(roomId, q.get("room_id"));
        assertEquals("protobuf", q.get("resp_content_type"));
        assertEquals("1988", q.get("aid"));
        assertEquals(Long.toString(Wss.HEARTBEAT_INTERVAL_MS), q.get("heartbeat_duration"));
        assertEquals("audience", q.get("identity"));
    }

    @Test
    void build_encodesSpecialCharactersInRoomId() {
        String roomId = "id+with spaces";
        String url = WssUrl.build("example.test", roomId, "en", "US");
        Map<String, String> q = parseQuery(url.substring(url.indexOf('?') + 1));
        assertEquals(roomId, q.get("room_id"));
    }

    private static Map<String, String> parseQuery(String raw) {
        var out = new LinkedHashMap<String, String>();
        for (String part : raw.split("&")) {
            int eq = part.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String k = URLDecoder.decode(part.substring(0, eq), StandardCharsets.UTF_8);
            String v = URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8);
            out.put(k, v);
        }
        return out;
    }
}
