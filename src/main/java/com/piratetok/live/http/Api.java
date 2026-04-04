package com.piratetok.live.http;

import com.piratetok.live.Errors.AgeRestrictedException;
import com.piratetok.live.Errors.HostNotOnlineException;
import com.piratetok.live.Errors.TikTokApiException;
import com.piratetok.live.Errors.TikTokBlockedException;
import com.piratetok.live.Errors.UserNotFoundException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

public final class Api {

    public record RoomIdResult(String roomId) {}

    public record StreamUrls(String flvOrigin, String flvHd, String flvSd, String flvLd, String flvAudio) {}

    public record RoomInfo(String title, int viewers, int likes, int totalUser, StreamUrls streamUrl) {}

    public static RoomIdResult checkOnline(String username, Duration timeout)
            throws IOException, InterruptedException {
        String clean = username.strip().replaceFirst("^@", "");
        String params = encodeParams(Map.of(
            "aid", TikTokWeb.PARAM_AID,
            "app_name", TikTokWeb.PARAM_APP_NAME,
            "device_platform", TikTokWeb.PARAM_DEVICE_PLATFORM_WEB_PC,
            "app_language", "en", "browser_language", "en-US", "user_is_login", "false",
            "sourceType", TikTokWeb.PARAM_USER_ROOM_SOURCE_TYPE,
            "staleTime", TikTokWeb.PARAM_USER_ROOM_STALE_TIME_MS,
            "uniqueId", clean
        ));
        String url = "https://www.tiktok.com/api-live/user/room?" + params;

        String body = httpGet(url, "", timeout);
        Map<String, Object> result = Json.parseObject(body);

        long statusCode = longVal(result, "statusCode");
        if (statusCode == TikTokWeb.JSON_STATUS_CODE_USER_NOT_FOUND) {
            throw new UserNotFoundException(clean);
        }
        if (statusCode != 0) throw new TikTokApiException(statusCode);

        @SuppressWarnings("unchecked")
        var data = (Map<String, Object>) result.getOrDefault("data", Map.of());
        @SuppressWarnings("unchecked")
        var user = (Map<String, Object>) data.getOrDefault("user", Map.of());
        @SuppressWarnings("unchecked")
        var liveRoom = (Map<String, Object>) data.getOrDefault("liveRoom", Map.of());

        String roomId = String.valueOf(user.getOrDefault("roomId", ""));
        if (roomId.isEmpty() || "0".equals(roomId)) throw new HostNotOnlineException(clean);

        long liveStatus = longVal(liveRoom, "status");
        long userStatus = longVal(user, "status");
        if (liveStatus != TikTokWeb.LIVE_STATUS_ON_AIR && userStatus != TikTokWeb.LIVE_STATUS_ON_AIR) {
            throw new HostNotOnlineException(clean);
        }

        return new RoomIdResult(roomId);
    }

    public static RoomInfo fetchRoomInfo(String roomId, Duration timeout, String cookies)
            throws IOException, InterruptedException {
        String params = encodeParams(Map.ofEntries(
            Map.entry("aid", TikTokWeb.PARAM_AID),
            Map.entry("app_name", TikTokWeb.PARAM_APP_NAME),
            Map.entry("device_platform", TikTokWeb.PARAM_DEVICE_PLATFORM_WEB_PC),
            Map.entry("app_language", "en"),
            Map.entry("browser_language", "en-US"), Map.entry("browser_name", "Mozilla"),
            Map.entry("browser_online", "true"), Map.entry("browser_platform", "Linux x86_64"),
            Map.entry("cookie_enabled", "true"),
            Map.entry("screen_height", TikTokWeb.SCREEN_HEIGHT),
            Map.entry("screen_width", TikTokWeb.SCREEN_WIDTH),
            Map.entry("tz_name", UserAgent.systemTimezone()),
            Map.entry("webcast_language", "en"), Map.entry("room_id", roomId)
        ));
        String url = "https://webcast.tiktok.com/webcast/room/info/?" + params;

        String body = httpGet(url, cookies, timeout);
        Map<String, Object> result = Json.parseObject(body);

        long sc = longVal(result, "status_code");
        if (sc == TikTokWeb.WEBCAST_STATUS_CODE_AGE_RESTRICTED) {
            throw new AgeRestrictedException();
        }
        if (sc != 0) throw new TikTokApiException(sc);

        @SuppressWarnings("unchecked")
        var data = (Map<String, Object>) result.getOrDefault("data", Map.of());
        @SuppressWarnings("unchecked")
        var stats = (Map<String, Object>) data.getOrDefault("stats", Map.of());

        return new RoomInfo(
            strVal(data, "title"),
            (int) longVal(data, "user_count"),
            (int) longVal(stats, "like_count"),
            (int) longVal(stats, "total_user"),
            parseStreamUrls(data.get("stream_url"))
        );
    }

    @SuppressWarnings("unchecked")
    private static StreamUrls parseStreamUrls(Object raw) {
        if (!(raw instanceof Map<?, ?> m)) return null;
        Object flvObj = m.get("flv_pull_url");
        if (!(flvObj instanceof Map<?, ?> flv)) return null;
        var f = (Map<String, Object>) flv;
        return new StreamUrls(
            strOr(f, "FULL_HD1"), strOr(f, "HD1"), strOr(f, "SD1"),
            strOr(f, "SD2"), strOr(f, "AUDIO")
        );
    }

    private static String httpGet(String url, String cookies, Duration timeout)
            throws IOException, InterruptedException {
        var builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", UserAgent.randomUa())
            .header("Referer", "https://www.tiktok.com/")
            .timeout(timeout)
            .GET();
        if (cookies != null && !cookies.isEmpty()) {
            builder.header("Cookie", cookies);
        }
        var resp = SharedHttpClient.instance()
                .send(builder.build(), HttpResponse.BodyHandlers.ofString());
        int status = resp.statusCode();
        if (status == TikTokWeb.HTTP_STATUS_FORBIDDEN || status == TikTokWeb.HTTP_STATUS_TOO_MANY_REQUESTS) {
            throw new TikTokBlockedException(status);
        }
        return resp.body();
    }

    private static long longVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s) { try { return Long.parseLong(s); } catch (NumberFormatException e) { return 0; } }
        return 0;
    }

    private static String strVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : "";
    }

    private static String strOr(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : "";
    }

    private static String encodeParams(Map<String, String> params) {
        var sb = new StringBuilder();
        for (var e : params.entrySet()) {
            if (!sb.isEmpty()) sb.append('&');
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
              .append('=')
              .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private Api() {}
}
