package com.piratetok.live.connection;

import com.piratetok.live.Limits;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FramesTest {

    @Test
    void decompressIfGzipped_roundTripSmallPayload() throws IOException {
        byte[] plain = "hello tiktok".getBytes();
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(compressed)) {
            gz.write(plain);
        }
        byte[] gzipBytes = compressed.toByteArray();
        assertArrayEquals(plain, Frames.decompressIfGzipped(gzipBytes));
    }

    @Test
    void decompressIfGzipped_nonGzipPassthrough() throws IOException {
        byte[] raw = {1, 2, 3, 4};
        assertArrayEquals(raw, Frames.decompressIfGzipped(raw));
    }

    @Test
    void decompressIfGzipped_rejectsOversizedOutput() throws IOException {
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(compressed)) {
            byte[] chunk = new byte[8192];
            Arrays.fill(chunk, (byte) 'A');
            int target = Limits.MAX_GZIP_DECOMPRESSED_BYTES + 8192;
            for (int written = 0; written < target; written += chunk.length) {
                gz.write(chunk);
            }
        }
        byte[] gzipBytes = compressed.toByteArray();
        assertThrows(IOException.class, () -> Frames.decompressIfGzipped(gzipBytes));
    }
}
