package com.piratetok.live.proto;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
