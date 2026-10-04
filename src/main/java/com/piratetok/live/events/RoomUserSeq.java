package com.piratetok.live.events;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Helpers over a decoded {@link EventType#ROOM_USER_SEQ} event's data. */
public final class RoomUserSeq {

    /**
     * The top-viewers box next to the viewer counter (usually the top 3 ranked by contribution score).
     * Entries without a decoded user are skipped; the rest come back sorted by rank. No cookies needed.
     *
     * @return contributor maps with {@code score}, {@code user}, {@code rank}, {@code delta}
     */
    public static List<Map<String, Object>> topViewers(Map<String, Object> data) {
        var top = new ArrayList<Map<String, Object>>();
        if (data.get("ranksList") instanceof List<?> ranks) {
            for (Object r : ranks) {
                if (r instanceof Map<?, ?> m && m.get("user") instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    var contributor = (Map<String, Object>) m;
                    top.add(contributor);
                }
            }
        }
        top.sort(Comparator.comparingLong(c -> c.get("rank") instanceof Number n ? n.longValue() : 0L));
        return top;
    }

    private RoomUserSeq() {}
}
