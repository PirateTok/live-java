package com.piratetok.live.events;

import java.util.Map;

/** Helpers over a decoded {@link EventType#GIFT} event's data. */
public final class Gift {

    /** Combo gifts (gift type 1) send several events with a running repeatCount until repeatEnd. */
    public static boolean isComboGift(Map<String, Object> data) {
        return num(details(data).get("type")) == 1;
    }

    /** Always true for non-combo gifts; true on repeatEnd == 1 for combos. */
    public static boolean isStreakOver(Map<String, Object> data) {
        return !isComboGift(data) || num(data.get("repeatEnd")) == 1;
    }

    /** Diamonds per gift × repeatCount (at least 1). */
    public static long diamondTotal(Map<String, Object> data) {
        return num(details(data).get("diamondCount")) * Math.max(1L, num(data.get("repeatCount")));
    }

    private static Map<?, ?> details(Map<String, Object> data) {
        return data.get("gift") instanceof Map<?, ?> g ? g : Map.of();
    }

    private static long num(Object v) {
        return v instanceof Number n ? n.longValue() : 0L;
    }

    private Gift() {}
}
