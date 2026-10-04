package com.piratetok.live.fakes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Offline HTTP CONNECT proxy: answers 407 (Basic challenge) without credentials, records target + credentials
 * once they arrive, then refuses the tunnel with 502. Nothing leaves the machine.
 */
public final class FakeConnectProxy implements AutoCloseable {

    public record Hit(String target, String auth) {}

    private final ServerSocket server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
    public final List<Hit> hits = new CopyOnWriteArrayList<>();

    public FakeConnectProxy() throws IOException {
        Thread.ofVirtual().start(this::acceptLoop);
    }

    public int port() {
        return server.getLocalPort();
    }

    private void acceptLoop() {
        while (!server.isClosed()) {
            try {
                Socket s = server.accept();
                Thread.ofVirtual().start(() -> serve(s));
            } catch (IOException closed) {
                return;
            }
        }
    }

    private void serve(Socket s) {
        try (s; InputStream in = s.getInputStream(); OutputStream out = s.getOutputStream()) {
            while (true) {
                Map<String, String> head = Http.readHead(in);
                if (head == null) {
                    return;
                }
                String auth = head.get("proxy-authorization");
                if (auth == null) {
                    out.write(("HTTP/1.1 407 Proxy Authentication Required\r\n"
                        + "Proxy-Authenticate: Basic realm=\"fake\"\r\nContent-Length: 0\r\n\r\n")
                        .getBytes(StandardCharsets.US_ASCII));
                    out.flush();
                    continue;
                }
                hits.add(new Hit(head.get("request-target"), auth));
                out.write("HTTP/1.1 502 Bad Gateway\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                    .getBytes(StandardCharsets.US_ASCII));
                return;
            }
        } catch (IOException dropped) {
            // client went away mid-request — nothing to record
        }
    }

    @Override
    public void close() throws IOException {
        server.close();
    }
}
