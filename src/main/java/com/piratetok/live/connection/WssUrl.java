package com.piratetok.live.connection;

import com.piratetok.live.http.UserAgent;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class WssUrl {

    public static String build(String cdnHost, String roomId, String language, String region) {
        String lastRtt = String.format("%.3f", 100 + Math.random() * 100);
        String browserLang = language + "-" + region;

        Map<String, String> params = new LinkedHashMap<>();
        params.put("version_code", "180800");
        params.put("device_platform", "web");
        params.put("cookie_enabled", "true");
        params.put("screen_width", "1920");
        params.put("screen_height", "1080");
        params.put("browser_language", browserLang);
        params.put("browser_platform", "Linux x86_64");
        params.put("browser_name", "Mozilla");
        params.put("browser_version", "5.0 (X11)");
        params.put("browser_online", "true");
        params.put("tz_name", UserAgent.systemTimezone());
        params.put("app_name", "tiktok_web");
        params.put("sup_ws_ds_opt", "1");
        params.put("update_version_code", "2.0.0");
        params.put("compress", "gzip");
        params.put("webcast_language", language);
        params.put("ws_direct", "1");
        params.put("aid", "1988");
        params.put("live_id", "12");
        params.put("app_language", language);
        params.put("client_enter", "1");
        params.put("room_id", roomId);
        params.put("identity", "audience");
        params.put("history_comment_count", "6");
        params.put("last_rtt", lastRtt);
        params.put("heartbeat_duration", "10000");
        params.put("resp_content_type", "protobuf");
        params.put("did_rule", "3");

        var sb = new StringBuilder();
        for (var e : params.entrySet()) {
            if (!sb.isEmpty()) sb.append('&');
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
              .append('=')
              .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }

        return "wss://" + cdnHost + "/webcast/im/ws_proxy/ws_reuse_supplement/?" + sb;
    }

    private WssUrl() {}
}
