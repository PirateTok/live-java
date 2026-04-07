package com.piratetok.live.http;

import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ThreadLocalRandom;

/**
 * User agent rotation pool, system timezone detection, and locale detection.
 *
 * <p>Each reconnect picks a random UA from the pool to reduce DEVICE_BLOCKED
 * risk. Timezone is detected from JVM default. Locale is detected from
 * {@link Locale#getDefault()}.</p>
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
    private static final String FALLBACK_LANG = "en";
    private static final String FALLBACK_REGION = "US";

    public static String randomUa() {
        int idx = ThreadLocalRandom.current().nextInt(USER_AGENTS.length);
        return USER_AGENTS[idx];
    }

    public static String systemTimezone() {
        String tz = TimeZone.getDefault().getID();
        if (tz == null || tz.isEmpty()) {
            return FALLBACK_TZ;
        }
        return tz;
    }

    /** Returns {@code [language, region]} from system locale, e.g. {@code ["ro", "RO"]}. */
    public static String[] systemLocale() {
        Locale loc = Locale.getDefault();
        String lang = loc.getLanguage();
        String region = loc.getCountry();
        if (lang == null || lang.length() < 2) lang = FALLBACK_LANG;
        if (region == null || region.length() < 2) region = FALLBACK_REGION;
        return new String[] { lang.toLowerCase(Locale.ROOT), region.toUpperCase(Locale.ROOT) };
    }

    /** System language code, e.g. {@code "en"}, {@code "ro"}. */
    public static String systemLanguage() { return systemLocale()[0]; }

    /** System region/country code, e.g. {@code "US"}, {@code "RO"}. */
    public static String systemRegion() { return systemLocale()[1]; }

    private UserAgent() {}
}
