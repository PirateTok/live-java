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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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
    private final AtomicBoolean stop = new AtomicBoolean(false);
    /** Current WSS session stop signal; replaced each reconnect attempt. */
    private final AtomicReference<CompletableFuture<Void>> activeSessionStop = new AtomicReference<>();
    private final Map<String, List<Consumer<TikTokEvent>>> listeners = new ConcurrentHashMap<>();

    public PirateTokClient(String username) {
        this.username = username;
    }

    public PirateTokClient cdnEU() { cdnHost = "webcast-ws.eu.tiktok.com"; return this; }
    public PirateTokClient cdnUS() { cdnHost = "webcast-ws.us.tiktok.com"; return this; }
    public PirateTokClient cdn(String host) { cdnHost = host; return this; }
    public PirateTokClient timeout(Duration t) { timeout = t; return this; }
    public PirateTokClient maxRetries(int n) { maxRetries = n; return this; }
    public PirateTokClient staleTimeout(Duration t) { staleTimeout = t; return this; }

    /**
     * Override the user agent for all requests (HTTP + WSS).
     *
     * <p>When not set, a random UA from the built-in pool is picked on each
     * reconnect attempt. This is recommended for reducing DEVICE_BLOCKED risk.
     * Only set this if you have a specific UA you want to use.</p>
     */
    public PirateTokClient userAgent(String ua) { this.userAgent = ua; return this; }

    /**
     * Set session cookies for the WSS connection.
     *
     * <p>These are appended alongside the ttwid cookie. Only needed if you have
     * a specific reason to pass session cookies to the WebSocket handshake.</p>
     *
     * <p>For fetching room info on 18+ rooms, pass cookies directly to
     * {@link #fetchRoomInfo(String, Duration, String)} instead.</p>
     */
    public PirateTokClient cookies(String cookies) { this.cookies = cookies; return this; }

    public PirateTokClient on(String eventType, Consumer<TikTokEvent> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
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
            var sessionStop = new CompletableFuture<Void>();
            activeSessionStop.set(sessionStop);
            if (stop.get()) {
                break;
            }

            // Pick UA: user override or random from pool (fresh each attempt)
            String ua = (userAgent != null && !userAgent.isEmpty()) ? userAgent : UserAgent.randomUa();
            String ttwid = Ttwid.fetch(timeout, ua);
            String wssUrl = WssUrl.build(cdnHost, room.roomId());

            if (stop.get()) {
                break;
            }

            boolean deviceBlocked = false;
            try {
                Wss.connect(wssUrl, ttwid, room.roomId(), staleTimeout, ua, cookies,
                    this::emit,
                    e -> emit(new TikTokEvent(EventType.ERROR, Map.of("error", e.getMessage()))),
                    stop,
                    sessionStop);
            } catch (DeviceBlockedException dbe) {
                deviceBlocked = true;
                log.warning("DEVICE_BLOCKED — rotating ttwid + UA");
            }

            if (stop.get()) break;

            attempt++;
            if (attempt > maxRetries) break;

            // On DEVICE_BLOCKED: short delay (2s) since we're getting a fresh
            // ttwid + UA anyway. On other errors: exponential backoff.
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

    public void disconnect() {
        stop.set(true);
        CompletableFuture<Void> f = activeSessionStop.get();
        if (f != null) {
            f.complete(null);
        }
    }

    public static RoomIdResult checkOnline(String username, Duration timeout)
            throws IOException, InterruptedException {
        return Api.checkOnline(username, timeout);
    }

    public static RoomInfo fetchRoomInfo(String roomId, Duration timeout, String cookies)
            throws IOException, InterruptedException {
        return Api.fetchRoomInfo(roomId, timeout, cookies);
    }
}
