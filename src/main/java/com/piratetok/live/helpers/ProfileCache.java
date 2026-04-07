package com.piratetok.live.helpers;

import com.piratetok.live.Errors.PirateTokException;
import com.piratetok.live.Errors.ProfileErrorException;
import com.piratetok.live.Errors.ProfileNotFoundException;
import com.piratetok.live.Errors.ProfilePrivateException;
import com.piratetok.live.auth.Ttwid;
import com.piratetok.live.http.Sigi;
import com.piratetok.live.http.Sigi.SigiProfile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cached profile fetcher that scrapes TikTok profile pages for HD avatars
 * and profile metadata.
 *
 * <p>Thread-safe via {@link ConcurrentHashMap}. Share freely across threads.</p>
 */
public final class ProfileCache {

    private static final Duration DEFAULT_TTL = Duration.ofSeconds(300);
    private static final Duration TTWID_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration SCRAPE_TIMEOUT = Duration.ofSeconds(15);

    private sealed interface CacheEntry permits CacheEntry.Ok, CacheEntry.Err {
        Instant insertedAt();
        record Ok(SigiProfile profile, Instant insertedAt) implements CacheEntry {}
        record Err(PirateTokException error, Instant insertedAt) implements CacheEntry {}
    }

    private final ConcurrentHashMap<String, CacheEntry> entries = new ConcurrentHashMap<>();
    private volatile String ttwid;
    private final Duration ttl;
    private final String proxy;
    private final String userAgent;
    private final String cookies;

    public ProfileCache() { this(DEFAULT_TTL, null, null, null); }

    public ProfileCache(Duration ttl) { this(ttl, null, null, null); }

    public ProfileCache(Duration ttl, String proxy, String userAgent, String cookies) {
        this.ttl = ttl;
        this.proxy = proxy;
        this.userAgent = userAgent;
        this.cookies = cookies;
    }

    /**
     * Fetch a profile, returning cached data if available and not expired.
     * Private/not-found profiles are negatively cached.
     */
    public SigiProfile fetch(String username) throws IOException, InterruptedException {
        String key = normalizeKey(username);

        CacheEntry cached = entries.get(key);
        if (cached != null && !isExpired(cached)) {
            if (cached instanceof CacheEntry.Ok ok) return ok.profile();
            if (cached instanceof CacheEntry.Err err) throw cloneError(err.error());
        }

        ensureTtwid();

        try {
            SigiProfile profile = Sigi.scrape(key, ttwid, SCRAPE_TIMEOUT, userAgent, cookies, proxy);
            entries.put(key, new CacheEntry.Ok(profile, Instant.now()));
            return profile;
        } catch (ProfilePrivateException | ProfileNotFoundException | ProfileErrorException e) {
            entries.put(key, new CacheEntry.Err(e, Instant.now()));
            throw e;
        }
    }

    /** Return cached profile without fetching. Returns {@code null} on miss or expiry. */
    public SigiProfile cached(String username) {
        String key = normalizeKey(username);
        CacheEntry entry = entries.get(key);
        if (entry instanceof CacheEntry.Ok ok && !isExpired(ok)) return ok.profile();
        return null;
    }

    /** Remove a single entry from the cache. */
    public void invalidate(String username) {
        entries.remove(normalizeKey(username));
    }

    /** Clear the entire cache. */
    public void invalidateAll() {
        entries.clear();
    }

    private boolean isExpired(CacheEntry entry) {
        return Duration.between(entry.insertedAt(), Instant.now()).compareTo(ttl) > 0;
    }

    private void ensureTtwid() throws IOException, InterruptedException {
        if (ttwid != null) return;
        ttwid = Ttwid.fetch(TTWID_TIMEOUT, userAgent, proxy);
    }

    private static String normalizeKey(String username) {
        return username.strip().replaceFirst("^@", "").toLowerCase(Locale.ROOT);
    }

    private static PirateTokException cloneError(PirateTokException e) {
        if (e instanceof ProfilePrivateException pp) return new ProfilePrivateException(pp.username);
        if (e instanceof ProfileNotFoundException pn) return new ProfileNotFoundException(pn.username);
        if (e instanceof ProfileErrorException pe) return new ProfileErrorException(pe.code);
        return e;
    }
}
