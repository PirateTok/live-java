package com.piratetok.live.http;

import java.util.TimeZone;
import java.util.concurrent.ThreadLocalRandom;

/**
 * User agent rotation pool and system timezone detection.
 *
 * <p>Each reconnect picks a random UA from the pool to reduce DEVICE_BLOCKED
 * risk. The timezone is detected from the JVM's default TimeZone.</p>
 */
public final class UserAgent {

    private static final String[] USER_AGENTS = {
        "Mozilla/5.0 (X11; Linux x86_64; rv:140.0) Gecko/20100101 Firefox/140.0",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:138.0) Gecko/20100101 Firefox/138.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 14.7; rv:139.0) Gecko/20100101 Firefox/139.0",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
    };

    private static final String FALLBACK_TZ = "UTC";

    /**
     * Pick a random user agent from the built-in pool.
     */
    public static String randomUa() {
        int idx = ThreadLocalRandom.current().nextInt(USER_AGENTS.length);
        return USER_AGENTS[idx];
    }

    /**
     * Detect the system's IANA timezone name.
     *
     * <p>Uses {@link TimeZone#getDefault()} and falls back to {@code "UTC"}
     * if the ID is null or empty.</p>
     */
    public static String systemTimezone() {
        String tz = TimeZone.getDefault().getID();
        if (tz == null || tz.isEmpty()) {
            return FALLBACK_TZ;
        }
        return tz;
    }

    private UserAgent() {}
}
