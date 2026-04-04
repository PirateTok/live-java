package com.piratetok.live.auth;

import com.piratetok.live.http.UserAgent;

import java.io.IOException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

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
        var cm = new CookieManager();
        try (var client = HttpClient.newBuilder().cookieHandler(cm).build()) {
            var req = HttpRequest.newBuilder()
                .uri(URI.create("https://www.tiktok.com/"))
                .header("User-Agent", ua)
                .timeout(timeout)
                .GET()
                .build();
            client.send(req, HttpResponse.BodyHandlers.discarding());

            for (HttpCookie cookie : cm.getCookieStore().getCookies()) {
                if ("ttwid".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        throw new IOException("ttwid: no ttwid cookie in response");
    }

    private Ttwid() {}
}
