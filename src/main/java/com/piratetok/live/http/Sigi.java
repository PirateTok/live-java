package com.piratetok.live.http;

import com.piratetok.live.Errors.ProfileErrorException;
import com.piratetok.live.Errors.ProfileNotFoundException;
import com.piratetok.live.Errors.ProfilePrivateException;
import com.piratetok.live.Errors.ProfileScrapeException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Sigi {

    private static final String SIGI_MARKER = "id=\"__UNIVERSAL_DATA_FOR_REHYDRATION__\"";

    public record SigiProfile(
        String userId, String uniqueId, String nickname, String bio,
        String avatarThumb, String avatarMedium, String avatarLarge,
        boolean verified, boolean privateAccount, boolean isOrganization,
        String roomId, String bioLink,
        long followerCount, long followingCount, long heartCount,
        long videoCount, long friendCount
    ) {}

    public static SigiProfile scrape(String username, String ttwid, Duration timeout,
            String userAgent, String cookies, String proxy) throws IOException, InterruptedException {
        String clean = username.strip().replaceFirst("^@", "").toLowerCase(Locale.ROOT);
        String ua = (userAgent != null && !userAgent.isEmpty()) ? userAgent : UserAgent.randomUa();

        String cookieHeader = buildCookie(ttwid, cookies);

        var req = HttpRequest.newBuilder()
            .uri(URI.create("https://www.tiktok.com/@" + clean))
            .header("User-Agent", ua)
            .header("Cookie", cookieHeader)
            .header("Accept-Language", acceptLanguage())
            .timeout(timeout)
            .GET()
            .build();

        String html;
        var clientBuilder = HttpClient.newBuilder();
        if (proxy != null && !proxy.isEmpty()) {
            URI proxyUri = URI.create(proxy);
            clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(proxyUri.getHost(), proxyUri.getPort())));
        }
        try (var client = clientBuilder.build()) {
            html = client.send(req, HttpResponse.BodyHandlers.ofString()).body();
        }

        String jsonStr = extractSigiJson(html);
        @SuppressWarnings("unchecked")
        var blob = (Map<String, Object>) Json.parse(jsonStr);

        @SuppressWarnings("unchecked")
        var scope = (Map<String, Object>) blob.get("__DEFAULT_SCOPE__");
        if (scope == null) throw new ProfileScrapeException("missing __DEFAULT_SCOPE__");

        @SuppressWarnings("unchecked")
        var userDetail = (Map<String, Object>) scope.get("webapp.user-detail");
        if (userDetail == null) throw new ProfileScrapeException("missing webapp.user-detail");

        long statusCode = longVal(userDetail, "statusCode");
        switch ((int) statusCode) {
            case 0 -> {}
            case 10222 -> throw new ProfilePrivateException(clean);
            case 10221, 10223 -> throw new ProfileNotFoundException(clean);
            default -> throw new ProfileErrorException(statusCode);
        }

        @SuppressWarnings("unchecked")
        var userInfo = (Map<String, Object>) userDetail.get("userInfo");
        if (userInfo == null) throw new ProfileScrapeException("missing userInfo");

        @SuppressWarnings("unchecked")
        var user = (Map<String, Object>) userInfo.get("user");
        if (user == null) throw new ProfileScrapeException("missing userInfo.user");

        @SuppressWarnings("unchecked")
        var stats = (Map<String, Object>) userInfo.get("stats");
        if (stats == null) throw new ProfileScrapeException("missing userInfo.stats");

        String bioLink = null;
        @SuppressWarnings("unchecked")
        var bioLinkObj = (Map<String, Object>) user.get("bioLink");
        if (bioLinkObj != null) {
            Object link = bioLinkObj.get("link");
            if (link instanceof String s && !s.isEmpty()) bioLink = s;
        }

        long isOrg = longVal(user, "isOrganization");

        return new SigiProfile(
            strVal(user, "id"), strVal(user, "uniqueId"), strVal(user, "nickname"),
            strVal(user, "signature"),
            strVal(user, "avatarThumb"), strVal(user, "avatarMedium"), strVal(user, "avatarLarger"),
            boolVal(user, "verified"), boolVal(user, "privateAccount"), isOrg != 0,
            strVal(user, "roomId"), bioLink,
            longVal(stats, "followerCount"), longVal(stats, "followingCount"),
            longVal(stats, "heartCount"), longVal(stats, "videoCount"), longVal(stats, "friendCount")
        );
    }

    private static String extractSigiJson(String html) {
        int markerPos = html.indexOf(SIGI_MARKER);
        if (markerPos < 0) throw new ProfileScrapeException("SIGI script tag not found in HTML");

        int gtPos = html.indexOf('>', markerPos);
        if (gtPos < 0) throw new ProfileScrapeException("no > after SIGI marker");

        int jsonStart = gtPos + 1;
        int scriptEnd = html.indexOf("</script>", jsonStart);
        if (scriptEnd < 0) throw new ProfileScrapeException("no </script> after SIGI JSON");

        String jsonStr = html.substring(jsonStart, scriptEnd);
        if (jsonStr.isEmpty()) throw new ProfileScrapeException("empty SIGI JSON blob");
        return jsonStr;
    }

    private static String buildCookie(String ttwid, String extra) {
        String base = "ttwid=" + ttwid;
        if (extra == null || extra.isEmpty()) return base;
        var sb = new StringBuilder();
        for (String pair : extra.split("; ")) {
            if (!pair.startsWith("ttwid=")) {
                if (!sb.isEmpty()) sb.append("; ");
                sb.append(pair);
            }
        }
        return sb.isEmpty() ? base : base + "; " + sb;
    }

    private static long longVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Number n) return n.longValue();
        return 0;
    }

    private static String strVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : "";
    }

    @SuppressWarnings("unchecked")
    private static boolean boolVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Boolean b) return b;
        return false;
    }

    private static String acceptLanguage() {
        String[] loc = UserAgent.systemLocale();
        return loc[0] + "-" + loc[1] + "," + loc[0] + ";q=0.9";
    }

    private Sigi() {}
}
