package com.piratetok.live.helpers;

import java.util.Map;

/**
 * Monotonizes TikTok's inconsistent {@code total_like_count}.
 *
 * <p>TikTok's {@code total} field on like events arrives from different server
 * shards with stale values, causing backwards jumps. The {@code count} field
 * (per-event delta) is reliable.</p>
 *
 * <p>Not thread-safe — use from a single event-handling thread.</p>
 */
public final class LikeAccumulator {

    public record LikeStats(
        int eventLikeCount,
        int totalLikeCount,
        long accumulatedCount,
        boolean wentBackwards
    ) {}

    private int maxTotal;
    private long accumulated;

    /**
     * Process a raw like event and return monotonized stats.
     *
     * @param data the decoded like event map (keys: count, total)
     */
    public LikeStats process(Map<String, Object> data) {
        int count = intVal(data, "count");
        int total = intVal(data, "total");

        accumulated += count;
        boolean wentBackwards = total < maxTotal;
        if (total > maxTotal) maxTotal = total;

        return new LikeStats(count, maxTotal, accumulated, wentBackwards);
    }

    /** Clear state. For reconnect. */
    public void reset() {
        maxTotal = 0;
        accumulated = 0;
    }

    private static int intVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Number n) return n.intValue();
        return 0;
    }
}
