package com.piratetok.live.auth;

import com.piratetok.live.http.SharedHttpClient;
import com.piratetok.live.http.UserAgent;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public final class Ttwid {

    /**
     * Fetch a fresh ttwid cookie using a random UA from the pool.
     */
    public static String fetch(Duration timeout) throws IOException, InterruptedException {
        return fetch(timeout, null, null);
    }

    /**
     * Fetch a fresh ttwid cookie.
     *
     * @param timeout  HTTP request timeout
     * @param userAgent  custom user agent, or {@code null} to pick a random one
     */
    public static String fetch(Duration timeout, String userAgent) throws IOException, InterruptedException {
        return fetch(timeout, userAgent, null);
    }

    /**
     * Fetch a fresh ttwid cookie.
     *
     * @param timeout    HTTP request timeout
     * @param userAgent  custom user agent, or {@code null} to pick a random one
     * @param proxy      proxy URL (e.g. "http://host:port"), or {@code null} for direct
     */
    public static String fetch(Duration timeout, String userAgent, String proxy) throws IOException, InterruptedException {
        String ua = (userAgent != null && !userAgent.isEmpty()) ? userAgent : UserAgent.randomUa();
        var req = HttpRequest.newBuilder()
                .uri(URI.create("https://www.tiktok.com/"))
                .header("User-Agent", ua)
                .timeout(timeout)
                .GET()
                .build();
        HttpResponse<?> resp;
        if (proxy != null && !proxy.isEmpty()) {
            URI proxyUri = URI.create(proxy);
            try (var client = HttpClient.newBuilder()
                    .proxy(ProxySelector.of(new InetSocketAddress(proxyUri.getHost(), proxyUri.getPort())))
                    .build()) {
                resp = client.send(req, HttpResponse.BodyHandlers.discarding());
            }
        } else {
            resp = SharedHttpClient.instance().send(req, HttpResponse.BodyHandlers.discarding());
        }
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
