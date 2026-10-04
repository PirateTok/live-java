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
import java.util.logging.Logger;

public final class Ttwid {

    private static final Logger log = Logger.getLogger(Ttwid.class.getName());
    private static final URI TIKTOK_URL = URI.create("https://www.tiktok.com/");

    /** TikTok only sets ttwid on ~1 in 5-8 anonymous GETs — retry when it's absent. */
    public static final int FETCH_ATTEMPTS = 8;
    public static final Duration RETRY_DELAY = Duration.ofMillis(750);

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
     * Fetch a fresh ttwid cookie, retrying up to {@link #FETCH_ATTEMPTS} times when the response
     * carries no ttwid. Transport errors propagate immediately.
     *
     * @param timeout    HTTP request timeout
     * @param userAgent  custom user agent, or {@code null} to pick a random one
     * @param proxy      proxy URL (e.g. "http://host:port"), or {@code null} for direct
     */
    public static String fetch(Duration timeout, String userAgent, String proxy) throws IOException, InterruptedException {
        String ua = (userAgent != null && !userAgent.isEmpty()) ? userAgent : UserAgent.randomUa();
        if (proxy == null || proxy.isEmpty()) {
            return fetch(SharedHttpClient.instance(), TIKTOK_URL, timeout, ua, FETCH_ATTEMPTS, RETRY_DELAY);
        }
        URI proxyUri = URI.create(proxy);
        try (var client = HttpClient.newBuilder()
                .proxy(ProxySelector.of(new InetSocketAddress(proxyUri.getHost(), proxyUri.getPort())))
                .build()) {
            return fetch(client, TIKTOK_URL, timeout, ua, FETCH_ATTEMPTS, RETRY_DELAY);
        }
    }

    static String fetch(HttpClient client, URI url, Duration timeout, String ua, int attempts, Duration delay)
            throws IOException, InterruptedException {
        var req = HttpRequest.newBuilder()
                .uri(url)
                .header("User-Agent", ua)
                .timeout(timeout)
                .GET()
                .build();
        for (int attempt = 1; ; attempt++) {
            HttpResponse<?> resp = client.send(req, HttpResponse.BodyHandlers.discarding());
            String ttwid = findTtwid(resp.headers().map().getOrDefault("set-cookie", List.of()));
            if (ttwid != null) {
                return ttwid;
            }
            if (attempt >= attempts) {
                throw new IOException("ttwid: no ttwid cookie after " + attempt
                        + " attempts (last HTTP " + resp.statusCode() + ")");
            }
            log.fine("ttwid: no cookie in response (attempt " + attempt + "), retrying");
            Thread.sleep(delay.toMillis());
        }
    }

    private static String findTtwid(List<String> setCookieHeaders) {
        for (String header : setCookieHeaders) {
            try {
                for (HttpCookie c : HttpCookie.parse(header)) {
                    if ("ttwid".equals(c.getName()) && !c.getValue().isEmpty()) {
                        return c.getValue();
                    }
                }
            } catch (IllegalArgumentException malformed) {
                log.fine("ttwid: skipping malformed Set-Cookie: " + malformed.getMessage());
            }
        }
        return null;
    }

    private Ttwid() {}
}
