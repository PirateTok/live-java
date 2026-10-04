package com.piratetok.live.events;

import com.piratetok.live.proto.Proto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomUserSeqTest {

    private static byte[] contributor(long rank, long score, long userId, String nick) {
        byte[] user = Proto.encode(w -> {
            w.writeInt64(1, userId);
            w.writeString(3, nick);
        });
        return Proto.encode(w -> {
            w.writeInt64(1, score);
            w.writeMessage(2, user);
            w.writeInt64(3, rank);
        });
    }

    @Test
    void decodesRanksListAndSortsTopViewersByRank() {
        byte[] noUser = Proto.encode(w -> w.writeInt64(1, 999));
        byte[] payload = Proto.encode(w -> {
            w.writeMessage(2, contributor(3, 10, 300, "third"));
            w.writeMessage(2, noUser);
            w.writeMessage(2, contributor(1, 5000, 100, "first"));
            w.writeMessage(2, contributor(2, 1200, 200, "second"));
            w.writeInt64(3, 321);
            w.writeInt64(7, 4567);
            w.writeInt64(8, 12);
        });

        List<TikTokEvent> events = Router.decode("WebcastRoomUserSeqMessage", payload, "1");
        assertEquals(1, events.size());
        Map<String, Object> data = events.getFirst().data();
        assertEquals(EventType.ROOM_USER_SEQ, events.getFirst().type());
        assertEquals(4, ((List<?>) data.get("ranksList")).size());
        assertEquals(321L, data.get("viewerCount"));
        assertEquals(4567, data.get("totalUser"));
        assertEquals(12L, data.get("anonymous"));

        var top = RoomUserSeq.topViewers(data);
        assertEquals(List.of(1L, 2L, 3L), top.stream().map(c -> c.get("rank")).toList());
        assertEquals(List.of("first", "second", "third"),
            top.stream().map(c -> ((Map<?, ?>) c.get("user")).get("nickname")).toList());
        assertEquals(5000L, top.getFirst().get("score"));
    }

    @Test
    void topViewersEmptyWithoutRanks() {
        assertTrue(RoomUserSeq.topViewers(Map.of()).isEmpty());
    }

    @Test
    void unknownKeepsMethodAndRawPayload() {
        byte[] raw = {1, 2, 3};
        var evt = Router.decode("WebcastKaraokeMessage", raw, "1").getFirst();
        assertEquals(EventType.UNKNOWN, evt.type());
        assertEquals("WebcastKaraokeMessage", evt.data().get("method"));
        assertEquals(raw, evt.data().get("payload"));
    }
}
