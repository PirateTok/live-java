package com.piratetok.live.proto;

import org.junit.jupiter.api.Test;

import com.piratetok.live.Limits;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtoTest {

    @Test
    void encodeDecode_stringAndVarint() {
        byte[] encoded = Proto.encode(w -> {
            w.writeString(1, "hello");
            w.writeInt64(2, 42L);
        });
        Proto.ProtoMap m = Proto.decode(encoded);
        assertEquals("hello", m.getString(1));
        assertEquals(42L, m.getVarint(2));
    }

    @Test
    void encodeDecode_nestedMessage() {
        byte[] inner = Proto.encode(w -> w.writeString(1, "inner"));
        byte[] outer = Proto.encode(w -> w.writeMessage(3, inner));
        Proto.ProtoMap root = Proto.decode(outer);
        Proto.ProtoMap nested = root.getMessage(3);
        assertEquals("inner", nested.getString(1));
    }

    @Test
    void encodeDecode_stringMap() {
        byte[] encoded = Proto.encode(w -> w.writeMap(5, Map.of("k1", "v1", "k2", "ab")));
        Proto.ProtoMap m = Proto.decode(encoded);
        Map<String, String> map = m.getStringMap(5);
        assertEquals("v1", map.get("k1"));
        assertEquals("ab", map.get("k2"));
    }

    @Test
    void encodeDecode_boolAndBytes() {
        byte[] raw = {0x01, 0x02, 0x03};
        byte[] encoded = Proto.encode(w -> {
            w.writeBool(1, true);
            w.writeBytes(2, raw);
        });
        Proto.ProtoMap m = Proto.decode(encoded);
        assertTrue(m.getBool(1));
        assertArrayEquals(raw, m.getRawBytes(2));
    }

    @Test
    void decode_emptyMessage() {
        Proto.ProtoMap m = Proto.decode(new byte[0]);
        assertTrue(m.fields().isEmpty());
    }

    @Test
    void getString_utf8RoundTrip() {
        String u = "测试 🎁";
        byte[] encoded = Proto.encode(w -> w.writeString(1, u));
        assertEquals(u, Proto.decode(encoded).getString(1));
        assertEquals(u, new String(Proto.decode(encoded).getRawBytes(1), StandardCharsets.UTF_8));
    }

    @Test
    void decode_rejectsLengthDelimitedOverMax() {
        long tooBig = (long) Limits.MAX_PROTO_FIELD_BYTES + 1;
        byte[] buf = new byte[16];
        buf[0] = 0x0a;
        int p = 1;
        long v = tooBig;
        while (v >= 0x80) {
            buf[p++] = (byte) ((v & 0x7F) | 0x80);
            v >>>= 7;
        }
        buf[p++] = (byte) v;
        byte[] payload = Arrays.copyOf(buf, p);
        assertThrows(IllegalArgumentException.class, () -> Proto.decode(payload));
    }

    @Test
    void decode_rejectsExcessiveNesting() {
        byte[] inner = Proto.encode(w -> w.writeString(1, "leaf"));
        for (int i = 0; i < 65; i++) {
            final byte[] prev = inner;
            inner = Proto.encode(w -> w.writeMessage(1, prev));
        }
        Proto.ProtoMap m = Proto.decode(inner);
        for (int i = 0; i < 64; i++) {
            m = m.getMessage(1);
        }
        Proto.ProtoMap deepest = m;
        assertThrows(IllegalArgumentException.class, () -> deepest.getMessage(1));
    }

    @Test
    void decode_rejectsOverlongVarint() {
        byte[] buf = new byte[24];
        for (int i = 0; i < buf.length; i++) {
            buf[i] = (byte) 0x80;
        }
        buf[buf.length - 1] = 0x01;
        assertThrows(IllegalArgumentException.class, () -> Proto.decode(buf));
    }
}
