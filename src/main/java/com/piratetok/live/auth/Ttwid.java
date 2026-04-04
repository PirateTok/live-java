package com.piratetok.live.auth;

import com.piratetok.live.http.SharedHttpClient;
import com.piratetok.live.http.UserAgent;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public final class Ttwid {

    /**
     * Fetch a fresh ttwid cookie using a random UA from the pool.
     */
    public static String fetch(Duration timeout) throws IOException, InterruptedException {
        return fetch(timeout, null);
    }

    /**
     * Fetch a fresh ttwid cookie.
     *
     * @param timeout  HTTP request timeout
     * @param userAgent  custom user agent, or {@code null} to pick a random one
     */
    public static String fetch(Duration timeout, String userAgent) throws IOException, InterruptedException {
        String ua = (userAgent != null && !userAgent.isEmpty()) ? userAgent : UserAgent.randomUa();
        var req = HttpRequest.newBuilder()
                .uri(URI.create("https://www.tiktok.com/"))
                .header("User-Agent", ua)
                .timeout(timeout)
                .GET()
                .build();
        var resp = SharedHttpClient.instance().send(req, HttpResponse.BodyHandlers.discarding());
        String ttwid = findTtwid(resp.headers().map().getOrDefault("set-cookie", List.of()));
        if (ttwid != null) {
            return ttwid;
        }
        throw new IOException("ttwid: no ttwid cookie in response");
    }

    private static String findTtwid(List<String> setCookieHeaders) {
        for (String header : setCookieHeaders) {
            try {
                for (HttpCookie c : HttpCookie.parse(header)) {
                    if ("ttwid".equals(c.getName())) {
                        return c.getValue();
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // malformed Set-Cookie line; try next
            }
        }
        return null;
    }

    private Ttwid() {}
}
