package com.piratetok.live.connection;

import com.piratetok.live.Limits;
import com.piratetok.live.proto.Proto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;

public final class Frames {

    public static byte[] buildHeartbeat(String roomId) {
        byte[] hb = Proto.encode(w -> w.writeUint64(1, Long.parseLong(roomId)));
        return Proto.encode(w -> {
            w.writeString(6, "pb");
            w.writeString(7, "hb");
            w.writeBytes(8, hb);
        });
    }

    public static byte[] buildEnterRoom(String roomId) {
        byte[] enter = Proto.encode(w -> {
            w.writeInt64(1, Long.parseLong(roomId));
            w.writeInt64(4, 12);
            w.writeString(5, "audience");
            w.writeString(9, "0");
        });
        return Proto.encode(w -> {
            w.writeString(6, "pb");
            w.writeString(7, "im_enter_room");
            w.writeBytes(8, enter);
        });
    }

    public static byte[] buildAck(long logId, byte[] internalExt) {
        return Proto.encode(w -> {
            w.writeString(6, "pb");
            w.writeString(7, "ack");
            w.writeUint64(2, logId);
            w.writeBytes(8, internalExt);
        });
    }

    public static byte[] decompressIfGzipped(byte[] data) throws IOException {
        if (data.length >= 2 && (data[0] & 0xFF) == 0x1F && (data[1] & 0xFF) == 0x8B) {
            var bais = new ByteArrayInputStream(data);
            var gis = new GZIPInputStream(bais);
            try (gis) {
                var bounded = new BoundedByteArrayOutputStream(Limits.MAX_GZIP_DECOMPRESSED_BYTES);
                gis.transferTo(bounded);
                return bounded.toByteArray();
            }
        }
        return data;
    }

    private static final class BoundedByteArrayOutputStream extends OutputStream {
        private final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        private final int maxBytes;

        BoundedByteArrayOutputStream(int maxBytes) {
            this.maxBytes = maxBytes;
        }

        @Override
        public void write(int b) throws IOException {
            if (buf.size() + 1 > maxBytes) {
                throw new IOException("decompressed gzip payload exceeds max size (" + maxBytes + " bytes)");
            }
            buf.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (len < 0 || off + len > b.length) {
                throw new IndexOutOfBoundsException();
            }
            if (buf.size() + len > maxBytes) {
                throw new IOException("decompressed gzip payload exceeds max size (" + maxBytes + " bytes)");
            }
            buf.write(b, off, len);
        }

        byte[] toByteArray() {
            return buf.toByteArray();
        }
    }

    private Frames() {}
}
