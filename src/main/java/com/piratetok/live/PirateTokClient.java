package com.piratetok.live;

import com.piratetok.live.Errors.DeviceBlockedException;
import com.piratetok.live.auth.Ttwid;
import com.piratetok.live.connection.Wss;
import com.piratetok.live.connection.WssUrl;
import com.piratetok.live.events.EventType;
import com.piratetok.live.events.TikTokEvent;
import com.piratetok.live.http.Api;
import com.piratetok.live.http.Api.RoomAudience;
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
    private static final int DEFAULT_HEARTBEAT_INTERVAL_SECONDS = 10;
    private static final int DEFAULT_MAX_RETRIES = 5;
    private static final int DEFAULT_STALE_TIMEOUT_SECONDS = 60;

    private final String username;
    private String cdnHost = "webcast-ws.tiktok.com";
    private Duration timeout = Duration.ofSeconds(DEFAULT_CONNECT_TIMEOUT_SECONDS);
    private Duration heartbeatInterval = Duration.ofSeconds(DEFAULT_HEARTBEAT_INTERVAL_SECONDS);
    private int maxRetries = DEFAULT_MAX_RETRIES;
    private Duration staleTimeout = Duration.ofSeconds(DEFAULT_STALE_TIMEOUT_SECONDS);
    private String userAgent;
    private String cookies;
    private String proxy;
    private boolean compress = true;
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

    /** Interval between WSS heartbeats (default 10s); also sent as the {@code heartbeat_duration} URL param. */
    public PirateTokClient heartbeatInterval(Duration d) { heartbeatInterval = d; return this; }

    /** Max consecutive failed attempts (default 5); a 30s healthy session resets the count. */
    public PirateTokClient maxRetries(int n) { maxRetries = n; return this; }
    public PirateTokClient staleTimeout(Duration t) { staleTimeout = t; return this; }
    public PirateTokClient userAgent(String ua) { this.userAgent = ua; return this; }
    public PirateTokClient cookies(String cookies) { this.cookies = cookies; return this; }

    /** Set proxy URL for all HTTP and WSS connections (e.g. {@code "http://host:port"}). */
    public PirateTokClient proxy(String proxy) { this.proxy = proxy; return this; }

    /** Enable or disable gzip compression for the WSS connection (default {@code true}). */
    public PirateTokClient compress(boolean enabled) { this.compress = enabled; return this; }

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

    /** ttwid + UA pair, reused across reconnects until rotated. */
    private record Session(String ttwid, String ua) {}

    private record Outcome(ReconnectBudget.Exit exit, Duration lived, Session session) {}

    /**
     * Connect synchronously (blocks the calling thread until disconnect or retry budget exhausted).
     */
    public String connect() throws Exception {
        try {
            return connectAsync().join();
        } catch (CompletionException ce) {
            Throwable c = ce.getCause();
            if (c instanceof RuntimeException re && re.getCause() instanceof Exception inner
                    && (inner instanceof IOException || inner instanceof InterruptedException)) {
                throw inner;
            }
            if (c instanceof Exception e) {
                throw e;
            }
            throw ce;
        }
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
                        return Api.checkOnline(username, timeout, language, region, proxy);
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
                    return sessionLoop(room, new ReconnectBudget(maxRetries), null, executor);
                });
    }

    /**
     * One attempt per call. ttwid + UA are fetched once and reused; they rotate only on DEVICE_BLOCKED,
     * a ttwid failure, or a connection that died young. A ttwid failure is a failed attempt, not an abort.
     */
    private CompletableFuture<String> sessionLoop(
            RoomIdResult room, ReconnectBudget budget, Session held, Executor executor) {
        if (stop.get()) {
            return finish(room);
        }
        var sessionStop = new CompletableFuture<Void>();
        activeSessionStop.set(sessionStop);
        if (stop.get()) {
            return finish(room);
        }

        CompletableFuture<Session> session = held != null
                ? CompletableFuture.completedFuture(held)
                : CompletableFuture.supplyAsync(this::freshSession, executor);

        return session.thenCompose(s -> runSession(room, s, sessionStop)).thenCompose(outcome -> {
            if (stop.get()) {
                return finish(room);
            }
            var judgement = ReconnectBudget.judge(outcome.exit(), outcome.lived());
            Session next = judgement.rotate() ? null : outcome.session();
            var verdict = budget.record(judgement.end());
            if (verdict.giveUp()) {
                log.info("max retries (" + maxRetries + ") exceeded at attempt " + verdict.attempt());
                return finish(room);
            }
            long delaySecs = verdict.delay().toSeconds();
            emit(new TikTokEvent(EventType.RECONNECTING,
                Map.of("attempt", verdict.attempt(), "maxRetries", maxRetries, "delaySecs", delaySecs),
                room.roomId()));
            log.info("reconnecting in " + delaySecs + "s (attempt " + verdict.attempt() + "/" + maxRetries + ")");
            var delayed = CompletableFuture.delayedExecutor(verdict.delay().toMillis(), TimeUnit.MILLISECONDS, executor);
            return CompletableFuture.runAsync(() -> {}, delayed)
                    .thenCompose(ignored -> sessionLoop(room, budget, next, executor));
        });
    }

    /** Returns {@code null} when the ttwid fetch failed. */
    private Session freshSession() {
        String ua = (userAgent != null && !userAgent.isEmpty()) ? userAgent : UserAgent.randomUa();
        try {
            return new Session(Ttwid.fetch(timeout, ua, proxy), ua);
        } catch (IOException e) {
            log.warning("ttwid acquisition failed: " + e.getMessage());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warning("ttwid acquisition interrupted");
            return null;
        }
    }

    private CompletableFuture<Outcome> runSession(RoomIdResult room, Session s, CompletableFuture<Void> sessionStop) {
        if (s == null) {
            return CompletableFuture.completedFuture(new Outcome(ReconnectBudget.Exit.NO_TTWID, Duration.ZERO, null));
        }
        if (stop.get()) {
            return CompletableFuture.completedFuture(new Outcome(ReconnectBudget.Exit.CLOSED, Duration.ZERO, s));
        }
        String wssUrl = WssUrl.build(cdnHost, room.roomId(), language, region, compress, heartbeatInterval);
        long started = System.nanoTime();
        return Wss.connectAsync(wssUrl, s.ttwid(), room.roomId(), heartbeatInterval, staleTimeout, s.ua(), cookies,
                acceptLanguage(), proxy,
                this::emit,
                e -> emit(new TikTokEvent(EventType.ERROR, Map.of("error", String.valueOf(e.getMessage())))),
                stop,
                sessionStop).handle((v, ex) -> {
            Duration lived = Duration.ofNanos(System.nanoTime() - started);
            return new Outcome(exitOf(ex), lived, s);
        });
    }

    private static ReconnectBudget.Exit exitOf(Throwable ex) {
        if (ex == null) {
            return ReconnectBudget.Exit.CLOSED;
        }
        Throwable c = ex instanceof CompletionException && ex.getCause() != null ? ex.getCause() : ex;
        if (c instanceof DeviceBlockedException) {
            log.warning("DEVICE_BLOCKED — rotating ttwid + UA");
            return ReconnectBudget.Exit.DEVICE_BLOCKED;
        }
        log.warning("websocket error: " + c);
        return ReconnectBudget.Exit.ERRORED;
    }

    private CompletableFuture<String> finish(RoomIdResult room) {
        emit(new TikTokEvent(EventType.DISCONNECTED, null, room.roomId()));
        return CompletableFuture.completedFuture(room.roomId());
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

    public static RoomIdResult checkOnline(String username, Duration timeout, String proxy)
            throws IOException, InterruptedException {
        return Api.checkOnline(username, timeout, null, null, proxy);
    }

    public static RoomInfo fetchRoomInfo(String roomId, Duration timeout, String cookies)
            throws IOException, InterruptedException {
        return Api.fetchRoomInfo(roomId, timeout, cookies);
    }

    public static RoomInfo fetchRoomInfo(String roomId, Duration timeout, String cookies, String proxy)
            throws IOException, InterruptedException {
        return Api.fetchRoomInfo(roomId, timeout, cookies, null, null, proxy);
    }

    /**
     * Full viewer roster. Login-gated: session cookies ({@code "sessionid=xxx; sid_tt=xxx"}) are required for
     * this call only, otherwise {@link Errors.SessionRequiredException}. Pass {@code null} anchorId to resolve
     * it from room info (one extra request).
     */
    public static RoomAudience fetchRoomAudience(String roomId, String anchorId, Duration timeout, String cookies)
            throws IOException, InterruptedException {
        return Api.fetchRoomAudience(roomId, anchorId, timeout, cookies, null, null, null);
    }
}
