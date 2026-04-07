package com.piratetok.live;

import com.piratetok.live.Errors.DeviceBlockedException;
import com.piratetok.live.auth.Ttwid;
import com.piratetok.live.connection.Wss;
import com.piratetok.live.connection.WssUrl;
import com.piratetok.live.events.EventType;
import com.piratetok.live.events.TikTokEvent;
import com.piratetok.live.http.Api;
import com.piratetok.live.http.Api.RoomIdResult;
import com.piratetok.live.http.Api.RoomInfo;
import com.piratetok.live.http.UserAgent;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class PirateTokClient {

    private static final Logger log = Logger.getLogger(PirateTokClient.class.getName());

    private final String username;
    private String cdnHost = "webcast-ws.tiktok.com";
    private Duration timeout = Duration.ofSeconds(10);
    private int maxRetries = 5;
    private Duration staleTimeout = Duration.ofSeconds(60);
    private String userAgent;
    private String cookies;
    private String language;
    private String region;
    private final AtomicBoolean stop = new AtomicBoolean(false);
    private final Map<String, List<Consumer<TikTokEvent>>> listeners = new ConcurrentHashMap<>();

    public PirateTokClient(String username) {
        this.username = username;
        String[] locale = UserAgent.systemLocale();
        this.language = locale[0];
        this.region = locale[1];
    }

    public PirateTokClient cdnEU() { cdnHost = "webcast-ws.eu.tiktok.com"; return this; }
    public PirateTokClient cdnUS() { cdnHost = "webcast-ws.us.tiktok.com"; return this; }
    public PirateTokClient cdn(String host) { cdnHost = host; return this; }
    public PirateTokClient timeout(Duration t) { timeout = t; return this; }
    public PirateTokClient maxRetries(int n) { maxRetries = n; return this; }
    public PirateTokClient staleTimeout(Duration t) { staleTimeout = t; return this; }
    public PirateTokClient userAgent(String ua) { this.userAgent = ua; return this; }
    public PirateTokClient cookies(String cookies) { this.cookies = cookies; return this; }

    /** Override detected language code for API requests and headers. */
    public PirateTokClient language(String lang) { this.language = lang; return this; }

    /** Override detected region/country code for API requests. */
    public PirateTokClient region(String region) { this.region = region; return this; }

    /** Returns browser_language value, e.g. {@code "en-US"}. */
    public String browserLanguage() { return language + "-" + region; }

    /** Returns Accept-Language header value, e.g. {@code "en-US,en;q=0.9"}. */
    public String acceptLanguage() { return language + "-" + region + "," + language + ";q=0.9"; }

    public PirateTokClient on(String eventType, Consumer<TikTokEvent> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
        return this;
    }

    private void emit(TikTokEvent event) {
        var handlers = listeners.get(event.type());
        if (handlers != null) {
            for (var h : handlers) h.accept(event);
        }
    }

    public String connect() throws Exception {
        var room = Api.checkOnline(username, timeout);
        stop.set(false);
        emit(new TikTokEvent(EventType.CONNECTED, Map.of("roomId", room.roomId()), room.roomId()));

        int attempt = 0;
        while (!stop.get()) {
            String ua = (userAgent != null && !userAgent.isEmpty()) ? userAgent : UserAgent.randomUa();
            String ttwid = Ttwid.fetch(timeout, ua);
            String wssUrl = WssUrl.build(cdnHost, room.roomId(), language, region);
            String acceptLang = acceptLanguage();

            boolean deviceBlocked = false;
            try {
                Wss.connect(wssUrl, ttwid, room.roomId(), staleTimeout, ua, cookies, acceptLang,
                    this::emit,
                    e -> emit(new TikTokEvent(EventType.ERROR, Map.of("error", e.getMessage()))),
                    stop);
            } catch (DeviceBlockedException dbe) {
                deviceBlocked = true;
                log.warning("DEVICE_BLOCKED — rotating ttwid + UA");
            }

            if (stop.get()) break;

            attempt++;
            if (attempt > maxRetries) break;

            long delay = deviceBlocked ? 2 : Math.min(1L << attempt, 30);

            emit(new TikTokEvent(EventType.RECONNECTING,
                Map.of("attempt", attempt, "maxRetries", maxRetries, "delaySecs", delay),
                room.roomId()));

            log.info("reconnecting in " + delay + "s (attempt " + attempt + "/" + maxRetries + ")");
            Thread.sleep(delay * 1000);
        }

        emit(new TikTokEvent(EventType.DISCONNECTED, null, room.roomId()));
        return room.roomId();
    }

    public void disconnect() { stop.set(true); }

    public static RoomIdResult checkOnline(String username, Duration timeout)
            throws IOException, InterruptedException {
        return Api.checkOnline(username, timeout);
    }

    public static RoomInfo fetchRoomInfo(String roomId, Duration timeout, String cookies)
            throws IOException, InterruptedException {
        return Api.fetchRoomInfo(roomId, timeout, cookies);
    }
}
