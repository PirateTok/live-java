package com.piratetok.live.connection;

import com.piratetok.live.http.UserAgent;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class WssUrl {

    private static final double LAST_RTT_MIN_MS = 100.0;
    private static final double LAST_RTT_RANDOM_SPREAD_MS = 100.0;

    private static final String VERSION_CODE = "180800";
    private static final String LIVE_ID = "12";
    private static final String HISTORY_COMMENT_COUNT = "6";
    private static final String DID_RULE = "3";
    private static final String CLIENT_ENTER = "1";
    private static final String FLAG_TRUE = "1";
    private static final String UPDATE_VERSION_CODE = "2.0.0";

    public static String build(String cdnHost, String roomId, String language, String region, boolean compress) {
        String lastRtt = String.format("%.3f", LAST_RTT_MIN_MS + Math.random() * LAST_RTT_RANDOM_SPREAD_MS);
        String browserLang = language + "-" + region;

        Map<String, String> params = new LinkedHashMap<>();
        params.put("version_code", VERSION_CODE);
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
        params.put("sup_ws_ds_opt", FLAG_TRUE);
        params.put("update_version_code", UPDATE_VERSION_CODE);
        params.put("compress", compress ? "gzip" : "");
        params.put("webcast_language", language);
        params.put("ws_direct", FLAG_TRUE);
        params.put("aid", "1988");
        params.put("live_id", LIVE_ID);
        params.put("app_language", language);
        params.put("client_enter", CLIENT_ENTER);
        params.put("room_id", roomId);
        params.put("identity", "audience");
        params.put("history_comment_count", HISTORY_COMMENT_COUNT);
        params.put("last_rtt", lastRtt);
        params.put("heartbeat_duration", Long.toString(Wss.HEARTBEAT_INTERVAL_MS));
        params.put("resp_content_type", "protobuf");
        params.put("did_rule", DID_RULE);

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
