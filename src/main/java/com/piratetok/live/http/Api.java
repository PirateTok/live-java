package com.piratetok.live.http;

import com.piratetok.live.Errors.AgeRestrictedException;
import com.piratetok.live.Errors.HostNotOnlineException;
import com.piratetok.live.Errors.InvalidResponseException;
import com.piratetok.live.Errors.SessionRequiredException;
import com.piratetok.live.Errors.TikTokApiException;
import com.piratetok.live.Errors.TikTokBlockedException;
import com.piratetok.live.Errors.UserNotFoundException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Api {

    private static final long STATUS_USER_NOT_FOUND = 19881007;
    private static final long STATUS_AGE_RESTRICTED = 4003110;
    private static final long STATUS_SESSION_REQUIRED = 20003;
    private static final int LIVE_STATUS_ON_AIR = 2;

    /** {@code anchorId} is the streamer's user ID ({@code data.user.id}); feeds {@link #fetchRoomAudience}. */
    public record RoomIdResult(String roomId, String anchorId) {}

    public record StreamUrls(String flvOrigin, String flvHd, String flvSd, String flvLd, String flvAudio) {}

    public record RoomInfo(String title, int viewers, int likes, int totalUser, StreamUrls streamUrl, String rawJson) {}

    public record RoomAudience(long total, long anonymous, List<AudienceViewer> viewers, String rawJson) {}

    /**
     * One named viewer. {@code avatarUrl} is {@code null} when TikTok sent none; {@code isFollower} = follows
     * the streamer, {@code isFollowing} = the streamer follows them.
     */
    public record AudienceViewer(long rank, long score, String userId, String username, String nickname,
            String secUid, String avatarUrl, long followerCount, boolean verified,
            boolean isFollower, boolean isFollowing, boolean isSubscriber) {}

    private record HttpResult(int status, String body) {}

    public static RoomIdResult checkOnline(String username, Duration timeout)
            throws IOException, InterruptedException {
        return checkOnline(username, timeout, null, null, null);
    }

    public static RoomIdResult checkOnline(String username, Duration timeout, String language, String region)
            throws IOException, InterruptedException {
        return checkOnline(username, timeout, language, region, null);
    }

    public static RoomIdResult checkOnline(String username, Duration timeout, String language, String region,
            String proxy) throws IOException, InterruptedException {
        String clean = username.strip().replaceFirst("^@", "");
        String[] loc = resolveLocale(language, region);
        String lang = loc[0];
        String reg = loc[1];
        String params = encodeParams(Map.of(
            "aid", "1988", "app_name", "tiktok_web", "device_platform", "web_pc",
            "app_language", lang, "browser_language", lang + "-" + reg, "region", reg,
            "user_is_login", "false", "sourceType", "54",
            "staleTime", "600000", "uniqueId", clean
        ));
        String url = "https://www.tiktok.com/api-live/user/room?" + params;

        HttpResult resp = httpGet(url, "", timeout, proxy);
        return parseCheckOnline(clean, resp.body(), resp.status());
    }

    static RoomIdResult parseCheckOnline(String username, String body, int httpStatus) {
        // empty / mangled / non-JSON = TikTok blocked the IP or fingerprint
        Map<String, Object> result;
        try {
            result = Json.parseObject(body);
        } catch (IllegalArgumentException notJson) {
            throw new TikTokBlockedException(httpStatus);
        }

        long statusCode = longVal(result, "statusCode");
        if (statusCode == STATUS_USER_NOT_FOUND) {
            throw new UserNotFoundException(username);
        }
        if (statusCode != 0) throw new TikTokApiException(statusCode);

        var data = obj(result, "data");
        var user = obj(data, "user");
        var liveRoom = obj(data, "liveRoom");

        String roomId = strVal(user, "roomId");
        if (roomId.isEmpty() || "0".equals(roomId)) throw new HostNotOnlineException(username);

        long liveStatus = longVal(liveRoom, "status");
        long userStatus = longVal(user, "status");
        if (liveStatus != LIVE_STATUS_ON_AIR && userStatus != LIVE_STATUS_ON_AIR) {
            throw new HostNotOnlineException(username);
        }

        return new RoomIdResult(roomId, strVal(user, "id"));
    }

    public static RoomInfo fetchRoomInfo(String roomId, Duration timeout, String cookies)
            throws IOException, InterruptedException {
        return fetchRoomInfo(roomId, timeout, cookies, null, null, null);
    }

    public static RoomInfo fetchRoomInfo(String roomId, Duration timeout, String cookies,
            String language, String region) throws IOException, InterruptedException {
        return fetchRoomInfo(roomId, timeout, cookies, language, region, null);
    }

    public static RoomInfo fetchRoomInfo(String roomId, Duration timeout, String cookies,
            String language, String region, String proxy) throws IOException, InterruptedException {
        String[] loc = resolveLocale(language, region);
        String lang = loc[0];
        String reg = loc[1];
        String params = encodeParams(Map.ofEntries(
            Map.entry("aid", "1988"), Map.entry("app_name", "tiktok_web"),
            Map.entry("device_platform", "web_pc"), Map.entry("app_language", lang),
            Map.entry("browser_language", lang + "-" + reg), Map.entry("browser_name", "Mozilla"),
            Map.entry("browser_online", "true"), Map.entry("browser_platform", "Linux x86_64"),
            Map.entry("cookie_enabled", "true"), Map.entry("screen_height", "1080"),
            Map.entry("screen_width", "1920"), Map.entry("tz_name", UserAgent.systemTimezone()),
            Map.entry("webcast_language", lang), Map.entry("room_id", roomId)
        ));
        String url = "https://webcast.tiktok.com/webcast/room/info/?" + params;

        String body = httpGet(url, cookies, timeout, proxy).body();
        Map<String, Object> result = Json.parseObject(body);

        long sc = longVal(result, "status_code");
        if (sc == STATUS_AGE_RESTRICTED) {
            throw new AgeRestrictedException();
        }
        if (sc != 0) throw new TikTokApiException(sc);

        var data = obj(result, "data");
        var stats = obj(data, "stats");

        return new RoomInfo(
            strVal(data, "title"),
            (int) longVal(data, "user_count"),
            (int) longVal(stats, "like_count"),
            (int) longVal(stats, "total_user"),
            parseStreamUrls(data.get("stream_url")),
            body
        );
    }

    /**
     * Fetch the full audience roster: every named viewer currently in the room — the whole viewer panel, not
     * just the top-3 box (for that, see {@code RoomUserSeq.topViewers}, which needs no cookies).
     *
     * <p>TikTok gates this endpoint behind a login: pass session cookies ({@code "sessionid=xxx; sid_tt=xxx"})
     * or you get {@link SessionRequiredException}. No ttwid, msToken, or signing needed.</p>
     *
     * @param anchorId the streamer's user ID ({@link RoomIdResult#anchorId()}), or {@code null} to resolve it
     *                 from room info (one extra request)
     */
    public static RoomAudience fetchRoomAudience(String roomId, String anchorId, Duration timeout, String cookies,
            String language, String region, String proxy) throws IOException, InterruptedException {
        String anchor = anchorId != null && !anchorId.isEmpty()
                ? anchorId
                : resolveAnchorId(fetchRoomInfo(roomId, timeout, cookies, language, region, proxy));
        String[] loc = resolveLocale(language, region);
        String params = encodeParams(Map.of(
            "aid", "1988", "app_name", "tiktok_web", "device_platform", "web_pc",
            "app_language", loc[0], "browser_language", loc[0] + "-" + loc[1],
            "channel", "tiktok_web", "room_id", roomId, "anchor_id", anchor
        ));
        String url = "https://webcast.tiktok.com/webcast/ranklist/online_audience/?" + params;
        HttpResult resp = httpGet(url, cookies, timeout, proxy);
        return parseRoomAudience(resp.body(), resp.status());
    }

    private static String resolveAnchorId(RoomInfo info) {
        String id = strVal(obj(obj(Json.parseObject(info.rawJson()), "data"), "owner"), "id_str");
        if (id.isEmpty()) {
            throw new InvalidResponseException("no owner id in room info");
        }
        return id;
    }

    static RoomAudience parseRoomAudience(String body, int httpStatus) {
        if (body == null || body.isEmpty()) {
            throw new InvalidResponseException("empty response from online_audience (HTTP " + httpStatus + ")");
        }
        Map<String, Object> result = Json.parseObject(body);
        if (!(result.get("status_code") instanceof Number code)) {
            throw new InvalidResponseException("no status_code in online_audience response");
        }
        var data = obj(result, "data");
        if (code.longValue() == STATUS_SESSION_REQUIRED) {
            throw new SessionRequiredException(
                "audience roster needs login — pass session cookies to fetchRoomAudience()");
        }
        if (code.longValue() != 0) {
            throw new InvalidResponseException(
                "online_audience status_code=" + code.longValue() + " " + strVal(data, "message"));
        }
        if (!(result.get("data") instanceof Map<?, ?>)) {
            throw new InvalidResponseException("missing 'data' in online_audience");
        }

        var viewers = new ArrayList<AudienceViewer>();
        if (data.get("ranks") instanceof List<?> ranks) {
            for (Object r : ranks) {
                if (r instanceof Map<?, ?> rank && rank.get("user") instanceof Map<?, ?>) {
                    viewers.add(audienceViewer(asMap(rank)));
                }
            }
        }
        return new RoomAudience(longVal(data, "total"), longVal(data, "anonymous"), List.copyOf(viewers), body);
    }

    private static AudienceViewer audienceViewer(Map<String, Object> rank) {
        var user = obj(rank, "user");
        String userId = strVal(user, "id_str");
        if (userId.isEmpty()) {
            userId = strVal(user, "id");
        }
        String avatar = null;
        if (obj(user, "avatar_thumb").get("url_list") instanceof List<?> urls
                && !urls.isEmpty() && urls.getFirst() instanceof String first) {
            avatar = first;
        }
        return new AudienceViewer(
            longVal(rank, "rank"), longVal(rank, "score"), userId,
            strVal(user, "display_id"), strVal(user, "nickname"), strVal(user, "sec_uid"), avatar,
            longVal(obj(user, "follow_info"), "follower_count"),
            boolVal(user, "verified"), boolVal(user, "is_follower"), boolVal(user, "is_following"),
            boolVal(user, "is_subscribe"));
    }

    @SuppressWarnings("unchecked")
    private static StreamUrls parseStreamUrls(Object raw) {
        if (!(raw instanceof Map<?, ?> m)) return null;
        Object flvObj = m.get("flv_pull_url");
        if (!(flvObj instanceof Map<?, ?> flv)) return null;
        var f = (Map<String, Object>) flv;
        return new StreamUrls(
            strVal(f, "FULL_HD1"), strVal(f, "HD1"), strVal(f, "SD1"),
            strVal(f, "SD2"), strVal(f, "AUDIO")
        );
    }

    private static HttpResult httpGet(String url, String cookies, Duration timeout, String proxy)
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
        HttpResponse<String> resp;
        if (proxy != null && !proxy.isEmpty()) {
            try (var client = ProxyConfig.apply(HttpClient.newBuilder(), proxy).build()) {
                resp = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            }
        } else {
            resp = SharedHttpClient.instance()
                    .send(builder.build(), HttpResponse.BodyHandlers.ofString());
        }
        int status = resp.statusCode();
        if (status == 403 || status == 429) {
            throw new TikTokBlockedException(status);
        }
        return new HttpResult(status, resp.body());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Map<?, ?> m) {
        return (Map<String, Object>) m;
    }

    private static Map<String, Object> obj(Map<String, Object> m, String key) {
        return m.get(key) instanceof Map<?, ?> nested ? asMap(nested) : Map.of();
    }

    private static long longVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s) { try { return Long.parseLong(s); } catch (NumberFormatException e) { return 0; } }
        return 0;
    }

    private static boolean boolVal(Map<String, Object> m, String key) {
        return Boolean.TRUE.equals(m.get(key));
    }

    private static String strVal(Map<String, Object> m, String key) {
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

    private static String[] resolveLocale(String language, String region) {
        String[] sys = UserAgent.systemLocale();
        String lang = (language != null && !language.isEmpty()) ? language : sys[0];
        String reg = (region != null && !region.isEmpty()) ? region : sys[1];
        return new String[] { lang, reg };
    }

    private Api() {}
}
