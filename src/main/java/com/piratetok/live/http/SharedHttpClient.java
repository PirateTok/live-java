package com.piratetok.live.http;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Single shared {@link HttpClient} for TikTok HTTP calls (API, ttwid, etc.).
 * Thread-safe; must not be closed per request.
 */
public final class SharedHttpClient {

    private static final int CONNECT_TIMEOUT_SECONDS = 20;

    private static final HttpClient INSTANCE = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
            .build();

    public static HttpClient instance() {
        return INSTANCE;
    }

    private SharedHttpClient() {}
}
