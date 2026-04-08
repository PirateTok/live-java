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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class PirateTokClient {

    private static final Logger log = Logger.getLogger(PirateTokClient.class.getName());

    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 10;
    private static final int DEFAULT_MAX_RETRIES = 5;
    private static final int DEFAULT_STALE_TIMEOUT_SECONDS = 60;

    private static final long DEVICE_BLOCKED_RECONNECT_DELAY_SECONDS = 2L;
    private static final long MAX_EXPONENTIAL_RECONNECT_BACKOFF_SECONDS = 30L;

    private final String username;
    private String cdnHost = "webcast-ws.tiktok.com";
    private Duration timeout = Duration.ofSeconds(DEFAULT_CONNECT_TIMEOUT_SECONDS);
    private int maxRetries = DEFAULT_MAX_RETRIES;
    private Duration staleTimeout = Duration.ofSeconds(DEFAULT_STALE_TIMEOUT_SECONDS);
    private String userAgent;
    private String cookies;
    private String language;
    private String region;
    private final AtomicBoolean stop = new AtomicBoolean(false);
    /** Current WSS session stop signal; replaced each reconnect attempt. */
    private final AtomicReference<CompletableFuture<Void>> activeSessionStop = new AtomicReference<>();
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
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
        return this;
    }

    private void emit(TikTokEvent event) {
        var handlers = listeners.get(event.type());
        if (handlers != null) {
            for (var h : handlers) h.accept(event);
        }
    }

    private enum SessionEnd {
        /** {@code stop} was set before opening WSS. */
        STOPPED,
        DEVICE_BLOCKED,
        NORMAL
    }

    private static long reconnectDelaySeconds(SessionEnd end, int attempt) {
        return end == SessionEnd.DEVICE_BLOCKED
                ? DEVICE_BLOCKED_RECONNECT_DELAY_SECONDS
                : Math.min(1L << attempt, MAX_EXPONENTIAL_RECONNECT_BACKOFF_SECONDS);
    }

    /**
     * One reconnect attempt: fetch ttwid, then block in {@link Wss#connect} until the session ends.
     *
     * @return {@link SessionEnd#STOPPED} if {@link #stop} before WSS; {@link SessionEnd#DEVICE_BLOCKED}
     *         on that handshake outcome; {@link SessionEnd#NORMAL} when the socket session finishes.
     */
    private SessionEnd runSingleWssSession(RoomIdResult room, CompletableFuture<Void> sessionStop)
            throws Exception {
        String ua = (userAgent != null && !userAgent.isEmpty()) ? userAgent : UserAgent.randomUa();
        String ttwid = Ttwid.fetch(timeout, ua);
        String wssUrl = WssUrl.build(cdnHost, room.roomId(), language, region);
        if (stop.get()) {
            return SessionEnd.STOPPED;
        }
        try {
            Wss.connect(wssUrl, ttwid, room.roomId(), staleTimeout, ua, cookies, acceptLanguage(),
                this::emit,
                e -> emit(new TikTokEvent(EventType.ERROR, Map.of("error", e.getMessage()))),
                stop,
                sessionStop);
            return SessionEnd.NORMAL;
        } catch (DeviceBlockedException dbe) {
            log.warning("DEVICE_BLOCKED — rotating ttwid + UA");
            return SessionEnd.DEVICE_BLOCKED;
        }
    }

    /**
     * Like {@link #runSingleWssSession} but uses {@link Wss#connectAsync} so no executor thread is held
     * for the whole WebSocket lifetime (only {@code ttwid} fetch and reconnect delays use the executor).
     */
    private CompletableFuture<SessionEnd> runSingleWssSessionAsync(
            RoomIdResult room, CompletableFuture<Void> sessionStop, Executor executor) {
        String ua = (userAgent != null && !userAgent.isEmpty()) ? userAgent : UserAgent.randomUa();
        return CompletableFuture.supplyAsync(() -> {
            try {
                return Ttwid.fetch(timeout, ua);
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new RuntimeException(e);
            }
        }, executor).thenCompose(ttwid -> {
            if (stop.get()) {
                return CompletableFuture.completedFuture(SessionEnd.STOPPED);
            }
            String wssUrl = WssUrl.build(cdnHost, room.roomId(), language, region);
            return Wss.connectAsync(wssUrl, ttwid, room.roomId(), staleTimeout, ua, cookies, acceptLanguage(),
                    this::emit,
                    e -> emit(new TikTokEvent(EventType.ERROR, Map.of("error", e.getMessage()))),
                    stop,
                    sessionStop).handle((v, ex) -> {
                if (ex == null) {
                    return SessionEnd.NORMAL;
                }
                Throwable c = ex instanceof CompletionException && ex.getCause() != null
                        ? ex.getCause() : ex;
                if (c instanceof DeviceBlockedException) {
                    log.warning("DEVICE_BLOCKED — rotating ttwid + UA");
                    return SessionEnd.DEVICE_BLOCKED;
                }
                if (c instanceof RuntimeException re) {
                    throw re;
                }
                throw new RuntimeException(c);
            });
        });
    }

    /**
     * Connect synchronously (blocks the calling thread until disconnect or retry budget exhausted).
     */
    public String connect() throws Exception {
        var room = Api.checkOnline(username, timeout, language, region);
        stop.set(false);
        emit(new TikTokEvent(EventType.CONNECTED, Map.of("roomId", room.roomId()), room.roomId()));

        int attempt = 0;
        while (!stop.get()) {
            var sessionStop = new CompletableFuture<Void>();
            activeSessionStop.set(sessionStop);
            if (stop.get()) {
                break;
            }

            SessionEnd end = runSingleWssSession(room, sessionStop);

            if (stop.get()) {
                break;
            }
            if (end == SessionEnd.STOPPED) {
                break;
            }

            attempt++;
            if (attempt > maxRetries) {
                break;
            }

            long delaySecs = reconnectDelaySeconds(end, attempt);

            emit(new TikTokEvent(EventType.RECONNECTING,
                Map.of("attempt", attempt, "maxRetries", maxRetries, "delaySecs", delaySecs),
                room.roomId()));

            log.info("reconnecting in " + delaySecs + "s (attempt " + attempt + "/" + maxRetries + ")");
            Thread.sleep(TimeUnit.SECONDS.toMillis(delaySecs));
        }

        emit(new TikTokEvent(EventType.DISCONNECTED, null, room.roomId()));
        return room.roomId();
    }

    /**
     * Connect without blocking the calling thread. Work runs on the {@link ForkJoinPool#commonPool()}.
     *
     * <p>For many concurrent streams, prefer {@link #connectAsync(Executor)} with a dedicated
     * {@link java.util.concurrent.Executors#newVirtualThreadPerTaskExecutor() virtual-thread}
     * (Java 21+) or a small bounded pool: the WebSocket session uses {@link Wss#connectAsync} and does
     * not hold an executor thread for the socket lifetime (only ttwid fetch and reconnect delays run on the executor).</p>
     *
     * @return completes with {@code roomId} when the client stops (disconnect or max retries), or
     *         completes exceptionally if e.g. {@link Api#checkOnline} fails
     */
    public CompletableFuture<String> connectAsync() {
        return connectAsync(ForkJoinPool.commonPool());
    }

    /**
     * Like {@link #connectAsync()} but uses the given executor for I/O and delayed reconnect steps.
     */
    public CompletableFuture<String> connectAsync(Executor executor) {
        Objects.requireNonNull(executor, "executor");
        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return Api.checkOnline(username, timeout, language, region);
                    } catch (IOException | InterruptedException e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        throw new RuntimeException(e);
                    }
                }, executor)
                .thenCompose(room -> {
                    stop.set(false);
                    emit(new TikTokEvent(EventType.CONNECTED, Map.of("roomId", room.roomId()), room.roomId()));
                    return sessionLoopAsync(room, 0, executor);
                });
    }

    private CompletableFuture<String> sessionLoopAsync(RoomIdResult room, int finishedAttempts, Executor executor) {
        if (stop.get()) {
            emit(new TikTokEvent(EventType.DISCONNECTED, null, room.roomId()));
            return CompletableFuture.completedFuture(room.roomId());
        }
        var sessionStop = new CompletableFuture<Void>();
        activeSessionStop.set(sessionStop);
        if (stop.get()) {
            emit(new TikTokEvent(EventType.DISCONNECTED, null, room.roomId()));
            return CompletableFuture.completedFuture(room.roomId());
        }

        return runSingleWssSessionAsync(room, sessionStop, executor).thenCompose(end -> {
            if (stop.get()) {
                emit(new TikTokEvent(EventType.DISCONNECTED, null, room.roomId()));
                return CompletableFuture.completedFuture(room.roomId());
            }
            if (end == SessionEnd.STOPPED) {
                emit(new TikTokEvent(EventType.DISCONNECTED, null, room.roomId()));
                return CompletableFuture.completedFuture(room.roomId());
            }
            int attempt = finishedAttempts + 1;
            if (attempt > maxRetries) {
                emit(new TikTokEvent(EventType.DISCONNECTED, null, room.roomId()));
                return CompletableFuture.completedFuture(room.roomId());
            }
            long delaySecs = reconnectDelaySeconds(end, attempt);
            emit(new TikTokEvent(EventType.RECONNECTING,
                Map.of("attempt", attempt, "maxRetries", maxRetries, "delaySecs", delaySecs),
                room.roomId()));
            log.info("reconnecting in " + delaySecs + "s (attempt " + attempt + "/" + maxRetries + ")");
            var delayed = CompletableFuture.delayedExecutor(delaySecs, TimeUnit.SECONDS, executor);
            return CompletableFuture.runAsync(() -> {}, delayed)
                    .thenCompose(___ -> sessionLoopAsync(room, attempt, executor));
        });
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
