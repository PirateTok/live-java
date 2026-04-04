package com.piratetok.live.events;

import java.util.Map;

public record TikTokEvent(String type, Map<String, Object> data, String roomId) {
    public TikTokEvent(String type, Map<String, Object> data) {
        this(type, data, "");
    }
}
