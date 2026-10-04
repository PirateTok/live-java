package com.piratetok.live.fakes;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

/** Plain ws:// server, one client: manual RFC 6455 handshake and framing. */
public final class FakeWebcast implements AutoCloseable {

    private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private final ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
    private Socket socket;
    private DataInputStream in;
    private OutputStream out;
    public Map<String, String> head;

    public FakeWebcast() throws IOException {}

    public int port() {
        return server.getLocalPort();
    }

    public void accept() throws IOException, NoSuchAlgorithmException {
        socket = server.accept();
        socket.setSoTimeout(10_000);
        in = new DataInputStream(socket.getInputStream());
        out = socket.getOutputStream();
        head = Http.readHead(in);
        String accept = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1")
            .digest((head.get("sec-websocket-key") + GUID).getBytes(StandardCharsets.US_ASCII)));
        out.write(("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n"
            + "Sec-WebSocket-Accept: " + accept + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        out.flush();
    }

    /** Next binary message from the client (masked frames, fragments joined; pings answered). */
    public byte[] receiveBinary() throws IOException {
        var msg = new ByteArrayOutputStream();
        while (true) {
            int b0 = in.readUnsignedByte();
            int b1 = in.readUnsignedByte();
            int opcode = b0 & 0x0f;
            long len = b1 & 0x7f;
            if (len == 126) len = in.readUnsignedShort();
            else if (len == 127) len = in.readLong();
            byte[] mask = new byte[4];
            if ((b1 & 0x80) != 0) in.readFully(mask);
            byte[] payload = new byte[(int) len];
            in.readFully(payload);
            for (int i = 0; i < payload.length; i++) payload[i] ^= mask[i % 4];
            if (opcode == 0x8) throw new IOException("client closed");
            if (opcode == 0x9) { send(0xA, payload); continue; }
            if (opcode == 0xA) continue;
            msg.write(payload);
            if ((b0 & 0x80) != 0) return msg.toByteArray();
        }
    }

    public void sendBinary(byte[] payload) throws IOException {
        send(0x2, payload);
    }

    /** Server-initiated close (status 1000). */
    public void sendClose() throws IOException {
        send(0x8, new byte[] {0x03, (byte) 0xE8});
    }

    private void send(int opcode, byte[] payload) throws IOException {
        out.write(0x80 | opcode);
        if (payload.length < 126) {
            out.write(payload.length);
        } else {
            out.write(126);
            out.write(payload.length >>> 8);
            out.write(payload.length & 0xff);
        }
        out.write(payload);
        out.flush();
    }

    @Override
    public void close() throws IOException {
        if (socket != null) socket.close();
        server.close();
    }
}
