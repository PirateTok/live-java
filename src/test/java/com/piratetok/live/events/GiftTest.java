package com.piratetok.live.events;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GiftTest {

    @Test
    void comboStreakAndDiamonds() {
        Map<String, Object> combo = new HashMap<>(Map.of(
            "gift", Map.of("type", 1, "diamondCount", 5), "repeatCount", 7, "repeatEnd", 0));
        assertTrue(Gift.isComboGift(combo));
        assertFalse(Gift.isStreakOver(combo));
        assertEquals(35, Gift.diamondTotal(combo));
        combo.put("repeatEnd", 1);
        assertTrue(Gift.isStreakOver(combo));

        Map<String, Object> single = Map.of("gift", Map.of("type", 2, "diamondCount", 100));
        assertFalse(Gift.isComboGift(single));
        assertTrue(Gift.isStreakOver(single));
        assertEquals(100, Gift.diamondTotal(single));
        assertEquals(0, Gift.diamondTotal(Map.of()));
    }
}
