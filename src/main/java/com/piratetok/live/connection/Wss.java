package com.piratetok.live.connection;

import com.piratetok.live.Errors.DeviceBlockedException;
import com.piratetok.live.Limits;
import com.piratetok.live.events.Router;
import com.piratetok.live.events.TikTokEvent;
import com.piratetok.live.http.UserAgent;
import com.piratetok.live.proto.Proto;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TikTok Live WebSocket client.
 *
 * <p>Scheduler pool size: environment variable {@link #ENV_WSS_SCHEDULER_THREADS}
 * ({@code PIRATETOK_WSS_SCHEDULER_THREADS}): positive integer, default {@code 4}, max {@code 10000}.
 * Used for periodic per-connection maintenance (single timer: heartbeat + stale check).</p>
 */
public final class Wss {

    private static final Logger log = Logger.getLogger(Wss.class.getName());

    /**
     * Client heartbeat period in ms; must match webcast {@code heartbeat_duration} (see {@link WssUrl}).
     */
    public static final long HEARTBEAT_INTERVAL_MS = 10_000L;

    private static final long HEARTBEAT_MS = HEARTBEAT_INTERVAL_MS;

    /** How often to re-check for stale connections (no inbound data). */
    private static final long STALE_RECHECK_INTERVAL_MS = 5_000L;

    /** Env: {@value #ENV_WSS_SCHEDULER_THREADS} */
    public static final String ENV_WSS_SCHEDULER_THREADS = "PIRATETOK_WSS_SCHEDULER_THREADS";

    private static final int DEFAULT_WSS_SCHEDULER_THREADS = 4;
    private static final int MAX_WSS_SCHEDULER_THREADS = 10_000;

    /** RFC 6455 close code 1002 (protocol error); JDK {@code WebSocket} has no named constant for it. */
    private static final int WS_CLOSE_PROTOCOL_ERROR = 1002;

    /** Shared client for all WebSocket connections; thread-safe and must not be closed per session. */
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private static final AtomicInteger WSS_THREAD_SEQ = new AtomicInteger();

    /**
     * Daemon scheduler for all connections: heartbeat + stale checks. Do not shut down per session.
     */
    private static final ScheduledExecutorService WS_SCHEDULER = createWssScheduler();

    private static int readSchedulerPoolSize() {
        String raw = System.getenv(ENV_WSS_SCHEDULER_THREADS);
        if (raw == null || raw.isBlank()) {
            return DEFAULT_WSS_SCHEDULER_THREADS;
        }
        try {
            int n = Integer.parseInt(raw.strip());
            if (n < 1) {
                log.warning(ENV_WSS_SCHEDULER_THREADS + "=" + raw + " invalid; using default "
                        + DEFAULT_WSS_SCHEDULER_THREADS);
                return DEFAULT_WSS_SCHEDULER_THREADS;
            }
            if (n > MAX_WSS_SCHEDULER_THREADS) {
                log.warning(ENV_WSS_SCHEDULER_THREADS + "=" + n + " exceeds max " + MAX_WSS_SCHEDULER_THREADS
                        + "; capping");
                return MAX_WSS_SCHEDULER_THREADS;
            }
            return n;
        } catch (NumberFormatException e) {
            log.warning(ENV_WSS_SCHEDULER_THREADS + "=" + raw + " invalid; using default "
                    + DEFAULT_WSS_SCHEDULER_THREADS);
            return DEFAULT_WSS_SCHEDULER_THREADS;
        }
    }

    private static ScheduledExecutorService createWssScheduler() {
        int poolSize = readSchedulerPoolSize();
        ThreadFactory tf = r -> {
            Thread t = new Thread(r, "piratetok-wss-" + WSS_THREAD_SEQ.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
        var ex = new ScheduledThreadPoolExecutor(poolSize, tf);
        ex.setRemoveOnCancelPolicy(true);
        log.info("Wss scheduler: " + poolSize + " threads (" + ENV_WSS_SCHEDULER_THREADS + ")");
        return ex;
    }

    private static void sendBinaryAsync(WebSocket ws, byte[] payload, String context) {
        if (ws.isOutputClosed()) {
            return;
        }
        ws.sendBinary(ByteBuffer.wrap(payload), true).whenComplete((w, err) -> {
            if (err != null) {
                log.log(Level.FINE, "wss sendBinary (" + context + "): " + err.getMessage(), err);
            }
        });
    }

    private static void cancelMaintenance(AtomicReference<ScheduledFuture<?>> taskRef) {
        ScheduledFuture<?> f = taskRef.getAndSet(null);
        if (f != null) {
            f.cancel(false);
        }
    }

    private static void sendCloseAsync(WebSocket ws, int code, String reason) {
        if (ws.isOutputClosed()) {
            return;
        }
        ws.sendClose(code, reason).whenComplete((w, err) -> {
            if (err != null) {
                log.log(Level.FINE, "wss sendClose (" + reason + "): " + err.getMessage(), err);
            }
        });
    }

    private static Throwable unwrapCompletion(Throwable ex) {
        return ex instanceof CompletionException ce && ce.getCause() != null ? ce.getCause() : ex;
    }

    /**
     * Connect to the TikTok Live WSS endpoint without blocking a thread for the session lifetime.
     *
     * <p>Completes when the socket closes, {@code sessionStop} completes, or an error occurs.
     * The handshake still runs on the JDK HTTP client executor; use {@link #connect} if you need
     * a blocking API.</p>
     *
     * @param wssUrl         full WebSocket URL
     * @param ttwid          ttwid cookie value
     * @param roomId         room ID string
     * @param staleTimeout   close if no data for this duration
     * @param userAgent      user agent string, or {@code null} for random
     * @param cookies        extra cookies to append alongside ttwid, or {@code null}
     * @param acceptLanguage Accept-Language header value (e.g. {@code "en-US,en;q=0.9"})
     * @param onEvent        event callback
     * @param onError        error callback
     * @param stop           atomic flag to signal disconnect
     * @param sessionStop    completes when this session should end (e.g. user disconnect); new instance per attempt
     * @return completes normally when the session ends; completes exceptionally on handshake or socket errors
     */
    public static CompletableFuture<Void> connectAsync(
            String wssUrl, String ttwid, String roomId, java.time.Duration staleTimeout,
            String userAgent, String cookies, String acceptLanguage,
            Consumer<TikTokEvent> onEvent, Consumer<Exception> onError,
            AtomicBoolean stop,
            CompletableFuture<Void> sessionStop
    ) {
        if (sessionStop.isDone()) {
            return CompletableFuture.completedFuture(null);
        }

        String ua = (userAgent != null && !userAgent.isEmpty()) ? userAgent : UserAgent.randomUa();
        String cookieHeader = buildCookieHeader(ttwid, cookies);

        var doneFuture = new CompletableFuture<Void>();
        var binaryBuf = new java.io.ByteArrayOutputStream();
        var lastData = new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis());
        var lastHeartbeatSent = new AtomicLong(0L);
        var maintenanceTaskRef = new AtomicReference<ScheduledFuture<?>>();
        long staleMs = staleTimeout.toMillis();

        WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public void onOpen(WebSocket ws) {
                sendBinaryAsync(ws, Frames.buildHeartbeat(roomId), "open-heartbeat");
                sendBinaryAsync(ws, Frames.buildEnterRoom(roomId), "enter-room");
                lastHeartbeatSent.set(System.currentTimeMillis());

                ScheduledFuture<?> maintenance = WS_SCHEDULER.scheduleAtFixedRate(() -> {
                    if (stop.get() || ws.isOutputClosed()) {
                        return;
                    }
                    long now = System.currentTimeMillis();
                    if (now - lastData.get() > staleMs) {
                        log.info("stale: no data for " + staleMs + "ms, closing");
                        sendCloseAsync(ws, WebSocket.NORMAL_CLOSURE, "stale");
                        return;
                    }
                    if (now - lastHeartbeatSent.get() >= HEARTBEAT_MS) {
                        sendBinaryAsync(ws, Frames.buildHeartbeat(roomId), "heartbeat");
                        lastHeartbeatSent.set(now);
                    }
                }, STALE_RECHECK_INTERVAL_MS, STALE_RECHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);
                maintenanceTaskRef.set(maintenance);

                ws.request(1);
            }

            @Override
            public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer data, boolean last) {
                lastData.set(System.currentTimeMillis());
                byte[] chunk = new byte[data.remaining()];
                data.get(chunk);
                long nextSize = (long) binaryBuf.size() + chunk.length;
                if (nextSize > Limits.MAX_WSS_FRAME_BYTES) {
                    binaryBuf.reset();
                    onError.accept(new IOException(
                            "WebSocket message exceeds max size (" + Limits.MAX_WSS_FRAME_BYTES + " bytes)"));
                    sendCloseAsync(ws, WS_CLOSE_PROTOCOL_ERROR, "frame too large");
                    ws.request(1);
                    return null;
                }
                binaryBuf.write(chunk, 0, chunk.length);

                if (last) {
                    byte[] raw = binaryBuf.toByteArray();
                    binaryBuf.reset();
                    try {
                        if (raw.length > Limits.MAX_WSS_FRAME_BYTES) {
                            throw new IOException(
                                    "WebSocket message exceeds max size (" + Limits.MAX_WSS_FRAME_BYTES + " bytes)");
                        }
                        processFrame(raw, ws, roomId, onEvent);
                    } catch (IOException ex) {
                        onError.accept(ex);
                    } catch (RuntimeException ex) {
                        onError.accept(ex);
                    }
                }
                ws.request(1);
                return null;
            }

            @Override
            public CompletionStage<?> onClose(WebSocket ws, int code, String reason) {
                cancelMaintenance(maintenanceTaskRef);
                doneFuture.complete(null);
                return null;
            }

            @Override
            public void onError(WebSocket ws, Throwable error) {
                cancelMaintenance(maintenanceTaskRef);
                doneFuture.completeExceptionally(error instanceof Exception ex ? ex : new RuntimeException(error));
            }
        };

        return HTTP_CLIENT.newWebSocketBuilder()
                .header("Cookie", cookieHeader)
                .header("User-Agent", ua)
                .header("Origin", "https://www.tiktok.com")
                .header("Referer", "https://www.tiktok.com/")
                .header("Accept-Language", acceptLanguage)
                .header("Cache-Control", "no-cache")
                .buildAsync(URI.create(wssUrl), listener)
                .handle((ws, ex) -> {
                    if (ex != null) {
                        Throwable c = unwrapCompletion(ex);
                        if (c instanceof WebSocketHandshakeException hse) {
                            checkDeviceBlocked(hse);
                        }
                        if (c instanceof RuntimeException re) {
                            throw new CompletionException(re);
                        }
                        if (c instanceof Exception e) {
                            throw new CompletionException(e);
                        }
                        throw new CompletionException(c);
                    }
                    return ws;
                })
                .thenCompose(ws -> {
                    if (sessionStop.isDone()) {
                        cancelMaintenance(maintenanceTaskRef);
                        sendCloseAsync(ws, WebSocket.NORMAL_CLOSURE, "disconnect");
                        return CompletableFuture.completedFuture(null);
                    }
                    return awaitSessionEnd(doneFuture, sessionStop, stop, ws, maintenanceTaskRef);
                });
    }

    private static CompletableFuture<Void> awaitSessionEnd(
            CompletableFuture<Void> doneFuture,
            CompletableFuture<Void> sessionStop,
            AtomicBoolean stop,
            WebSocket ws,
            AtomicReference<ScheduledFuture<?>> maintenanceTaskRef) {
        return CompletableFuture.anyOf(doneFuture, sessionStop).handle((__, ex) -> {
            cancelMaintenance(maintenanceTaskRef);
            if (stop.get()) {
                sendCloseAsync(ws, WebSocket.NORMAL_CLOSURE, "disconnect");
            }
            if (ex != null) {
                Throwable c = unwrapCompletion(ex);
                if (c instanceof RuntimeException re) {
                    throw new CompletionException(re);
                }
                if (c instanceof Exception e) {
                    throw new CompletionException(e);
                }
                throw new CompletionException(c);
            }
            return null;
        });
    }

    /**
     * Connect to the TikTok Live WSS endpoint (blocks the calling thread until the session ends).
     *
     * @param wssUrl         full WebSocket URL
     * @param ttwid          ttwid cookie value
     * @param roomId         room ID string
     * @param staleTimeout   close if no data for this duration
     * @param userAgent      user agent string, or {@code null} for random
     * @param cookies        extra cookies to append alongside ttwid, or {@code null}
     * @param acceptLanguage Accept-Language header value (e.g. {@code "en-US,en;q=0.9"})
     * @param onEvent        event callback
     * @param onError        error callback
     * @param stop           atomic flag to signal disconnect
     * @param sessionStop    completes when this session should end (e.g. user disconnect); new instance per attempt
     * @throws DeviceBlockedException if handshake returns DEVICE_BLOCKED
     */
    public static void connect(
            String wssUrl, String ttwid, String roomId, java.time.Duration staleTimeout,
            String userAgent, String cookies, String acceptLanguage,
            Consumer<TikTokEvent> onEvent, Consumer<Exception> onError,
            AtomicBoolean stop,
            CompletableFuture<Void> sessionStop
    ) throws Exception {
        try {
            connectAsync(wssUrl, ttwid, roomId, staleTimeout, userAgent, cookies, acceptLanguage,
                    onEvent, onError, stop, sessionStop).join();
        } catch (CompletionException ce) {
            Throwable c = ce.getCause();
            if (c instanceof Exception ex) {
                throw ex;
            }
            if (c != null) {
                throw new Exception(c);
            }
            throw ce;
        }
    }

    /**
     * Check if a handshake failure is a DEVICE_BLOCKED response.
     *
     * @throws DeviceBlockedException if the Handshake-Msg header says DEVICE_BLOCKED
     */
    private static void checkDeviceBlocked(WebSocketHandshakeException hse) {
        HttpResponse<?> resp = hse.getResponse();
        if (resp == null) {
            return;
        }
        Optional<String> handshakeMsg = resp.headers().firstValue("Handshake-Msg");
        if (handshakeMsg.isPresent() && "DEVICE_BLOCKED".equals(handshakeMsg.get())) {
            throw new DeviceBlockedException();
        }
    }

    private static String buildCookieHeader(String ttwid, String extraCookies) {
        String base = "ttwid=" + ttwid;
        if (extraCookies != null && !extraCookies.isEmpty()) {
            return base + "; " + extraCookies;
        }
        return base;
    }

    private static void processFrame(byte[] raw, WebSocket ws, String roomId, Consumer<TikTokEvent> onEvent)
            throws IOException {
        var frame = Proto.decode(raw);
        String payloadType = frame.getString(7);

        if ("msg".equals(payloadType)) {
            byte[] payload = Frames.decompressIfGzipped(frame.getRawBytes(8));
            var response = Proto.decode(payload);

            boolean needsAck = response.getBool(9);
            byte[] internalExt = response.getRawBytes(5);
            if (needsAck && internalExt.length > 0) {
                long logId = frame.getVarint(2);
                byte[] ack = Frames.buildAck(logId, internalExt);
                sendBinaryAsync(ws, ack, "ack");
            }

            for (var msg : response.getRepeatedMessages(1)) {
                String method = msg.getString(1);
                byte[] msgPayload = msg.getRawBytes(2);
                for (var evt : Router.decode(method, msgPayload, roomId)) {
                    onEvent.accept(evt);
                }
            }
        }
    }

    private Wss() {}
}
