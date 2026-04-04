package com.piratetok.live.connection;

import com.piratetok.live.Errors.DeviceBlockedException;
import com.piratetok.live.events.EventType;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Wss {

    private static final Logger log = Logger.getLogger(Wss.class.getName());
    private static final long HEARTBEAT_MS = 10_000;

    /** Shared client for all WebSocket connections; thread-safe and must not be closed per session. */
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    /**
     * Connect to the TikTok Live WSS endpoint.
     *
     * @param wssUrl        full WebSocket URL
     * @param ttwid         ttwid cookie value
     * @param roomId        room ID string
     * @param staleTimeout  close if no data for this duration
     * @param userAgent     user agent string, or {@code null} for random
     * @param cookies       extra cookies to append alongside ttwid, or {@code null}
     * @param onEvent       event callback
     * @param onError       error callback
     * @param stop          atomic flag to signal disconnect
     * @throws DeviceBlockedException if handshake returns DEVICE_BLOCKED
     */
    public static void connect(
            String wssUrl, String ttwid, String roomId, java.time.Duration staleTimeout,
            String userAgent, String cookies,
            Consumer<TikTokEvent> onEvent, Consumer<Exception> onError,
            AtomicBoolean stop
    ) throws Exception {
        String ua = (userAgent != null && !userAgent.isEmpty()) ? userAgent : UserAgent.randomUa();
        String cookieHeader = buildCookieHeader(ttwid, cookies);

        var doneFuture = new CompletableFuture<Void>();
        ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(2);
        var binaryBuf = new java.io.ByteArrayOutputStream();
        var lastData = new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis());
        long staleMs = staleTimeout.toMillis();

        WebSocket.Listener listener = new WebSocket.Listener() {
            ScheduledFuture<?> heartbeatTask;

            @Override
            public void onOpen(WebSocket ws) {
                ws.sendBinary(ByteBuffer.wrap(Frames.buildHeartbeat(roomId)), true);
                ws.sendBinary(ByteBuffer.wrap(Frames.buildEnterRoom(roomId)), true);

                heartbeatTask = scheduler.scheduleAtFixedRate(() -> {
                    if (!stop.get() && !ws.isOutputClosed()) {
                        ws.sendBinary(ByteBuffer.wrap(Frames.buildHeartbeat(roomId)), true);
                    }
                }, HEARTBEAT_MS, HEARTBEAT_MS, TimeUnit.MILLISECONDS);

                ws.request(1);
            }

            @Override
            public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer data, boolean last) {
                lastData.set(System.currentTimeMillis());
                byte[] chunk = new byte[data.remaining()];
                data.get(chunk);
                binaryBuf.write(chunk, 0, chunk.length);

                if (last) {
                    byte[] raw = binaryBuf.toByteArray();
                    binaryBuf.reset();
                    try {
                        processFrame(raw, ws, roomId, onEvent);
                    } catch (IOException ex) {
                        onError.accept(ex);
                    }
                }
                ws.request(1);
                return null;
            }

            @Override
            public CompletionStage<?> onClose(WebSocket ws, int code, String reason) {
                if (heartbeatTask != null) heartbeatTask.cancel(false);
                scheduler.shutdown();
                doneFuture.complete(null);
                return null;
            }

            @Override
            public void onError(WebSocket ws, Throwable error) {
                if (heartbeatTask != null) heartbeatTask.cancel(false);
                scheduler.shutdown();
                doneFuture.completeExceptionally(error instanceof Exception ex ? ex : new RuntimeException(error));
            }
        };

        WebSocket ws;
        try {
            ws = HTTP_CLIENT.newWebSocketBuilder()
                .header("Cookie", cookieHeader)
                .header("User-Agent", ua)
                .header("Origin", "https://www.tiktok.com")
                .header("Referer", "https://www.tiktok.com/")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Cache-Control", "no-cache")
                .buildAsync(URI.create(wssUrl), listener)
                .join();
        } catch (CompletionException ce) {
            Throwable cause = ce.getCause();
            if (cause instanceof WebSocketHandshakeException hse) {
                checkDeviceBlocked(hse);
                throw hse;
            }
            if (cause instanceof Exception ex) {
                throw ex;
            }
            throw ce;
        }

        ScheduledFuture<?> staleChecker = scheduler.scheduleAtFixedRate(() -> {
            if (System.currentTimeMillis() - lastData.get() > staleMs) {
                log.info("stale: no data for " + staleMs + "ms, closing");
                if (!ws.isOutputClosed()) {
                    ws.sendClose(WebSocket.NORMAL_CLOSURE, "stale").join();
                }
            }
        }, staleMs, 5000, TimeUnit.MILLISECONDS);

        while (!doneFuture.isDone() && !stop.get()) {
            Thread.sleep(200);
        }
        staleChecker.cancel(false);
        if (stop.get() && !ws.isOutputClosed()) {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "disconnect").join();
        }
        scheduler.shutdownNow();
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
                if (!ws.isOutputClosed()) {
                    ws.sendBinary(ByteBuffer.wrap(ack), true);
                }
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
