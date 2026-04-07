package com.piratetok.live.helpers;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks gift streak deltas from TikTok's running totals.
 *
 * <p>TikTok combo gifts fire multiple events during a streak, each carrying
 * a running total in {@code repeatCount} (2, 4, 7, 7). This helper tracks
 * active streaks by {@code groupId} and computes the delta per event.</p>
 *
 * <p>Not thread-safe — use from a single event-handling thread.</p>
 */
public final class GiftStreakTracker {

    private static final long STALE_SECS = 60;

    public record GiftStreakEvent(
        long streakId,
        boolean isActive,
        boolean isFinal,
        int eventGiftCount,
        int totalGiftCount,
        long eventDiamondCount,
        long totalDiamondCount
    ) {}

    private record StreakEntry(int lastRepeatCount, Instant lastSeen) {}

    private final HashMap<Long, StreakEntry> streaks = new HashMap<>();

    /**
     * Process a raw gift event and return enriched streak data with deltas.
     *
     * @param data the decoded gift event map (keys: groupId, repeatCount,
     *             repeatEnd, gift.type, gift.diamondCount)
     */
    public GiftStreakEvent process(Map<String, Object> data) {
        long groupId = longVal(data, "groupId");
        int repeatCount = intVal(data, "repeatCount");
        int repeatEnd = intVal(data, "repeatEnd");

        @SuppressWarnings("unchecked")
        var giftStruct = (Map<String, Object>) data.getOrDefault("gift", Map.of());
        int giftType = intVal(giftStruct, "type");
        int diamondPer = intVal(giftStruct, "diamondCount");

        boolean isCombo = giftType == 1;
        boolean isFinal = repeatEnd == 1;

        if (!isCombo) {
            return new GiftStreakEvent(groupId, false, true, 1, 1, diamondPer, diamondPer);
        }

        Instant now = Instant.now();
        evictStale(now);

        int prevCount = 0;
        StreakEntry prev = streaks.get(groupId);
        if (prev != null) prevCount = prev.lastRepeatCount();

        int delta = Math.max(repeatCount - prevCount, 0);

        if (isFinal) {
            streaks.remove(groupId);
        } else {
            streaks.put(groupId, new StreakEntry(repeatCount, now));
        }

        long totalDiamonds = (long) diamondPer * Math.max(repeatCount, 1);
        long eventDiamonds = (long) diamondPer * delta;

        return new GiftStreakEvent(
            groupId, !isFinal, isFinal, delta, repeatCount, eventDiamonds, totalDiamonds
        );
    }

    /** Number of currently active (non-finalized) streaks. */
    public int activeStreaks() {
        return streaks.size();
    }

    /** Clear all tracked state. For reconnect scenarios. */
    public void reset() {
        streaks.clear();
    }

    private void evictStale(Instant now) {
        streaks.entrySet().removeIf(e ->
            java.time.Duration.between(e.getValue().lastSeen(), now).getSeconds() >= STALE_SECS
        );
    }

    private static int intVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Number n) return n.intValue();
        return 0;
    }

    private static long longVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Number n) return n.longValue();
        return 0;
    }
}
