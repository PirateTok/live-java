package com.piratetok.live.proto;

import com.piratetok.live.Limits;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Proto {

    // === Writer ===

    public static byte[] encode(java.util.function.Consumer<Writer> builder) {
        var w = new Writer();
        builder.accept(w);
        return w.toByteArray();
    }

    public static final class Writer {
        private final ByteArrayOutputStream buf = new ByteArrayOutputStream();

        public void writeVarint(long v) {
            while ((v & ~0x7FL) != 0) {
                buf.write((int) (v & 0x7F) | 0x80);
                v >>>= 7;
            }
            buf.write((int) v);
        }

        public void writeUint64(int field, long v) {
            writeVarint(((long) field << 3));
            writeVarint(v);
        }

        public void writeInt64(int field, long v) { writeUint64(field, v); }
        public void writeInt32(int field, int v) { writeUint64(field, v & 0xFFFFFFFFL); }
        public void writeBool(int field, boolean v) { writeUint64(field, v ? 1 : 0); }

        public void writeString(int field, String v) {
            writeByteArray(field, v.getBytes(StandardCharsets.UTF_8));
        }

        public void writeBytes(int field, byte[] v) { writeByteArray(field, v); }

        public void writeMessage(int field, byte[] encoded) { writeByteArray(field, encoded); }

        public void writeMap(int field, Map<String, String> map) {
            for (var e : map.entrySet()) {
                byte[] entry = encode(w -> {
                    w.writeString(1, e.getKey());
                    w.writeString(2, e.getValue());
                });
                writeMessage(field, entry);
            }
        }

        private void writeByteArray(int field, byte[] v) {
            writeVarint(((long) field << 3) | 2);
            writeVarint(v.length);
            buf.write(v, 0, v.length);
        }

        public byte[] toByteArray() { return buf.toByteArray(); }
    }

    // === Reader ===

    public static ProtoMap decode(byte[] data) {
        return decode(data, 0);
    }

    private static ProtoMap decode(byte[] data, int decodeDepth) {
        if (decodeDepth > Limits.MAX_PROTO_NEST_DEPTH) {
            throw new IllegalArgumentException(
                    "protobuf nesting exceeds max depth (" + Limits.MAX_PROTO_NEST_DEPTH + ")");
        }
        return new Reader(data, 0, data.length, decodeDepth).readMessage();
    }

    private static final class Reader {
        private static final int MAX_VARINT_BYTES = 10;

        private final byte[] data;
        private int pos;
        private final int end;
        private final int decodeDepth;

        Reader(byte[] data, int offset, int length, int decodeDepth) {
            this.data = data;
            this.pos = offset;
            this.end = offset + length;
            this.decodeDepth = decodeDepth;
        }

        long readVarint() {
            long result = 0;
            int shift = 0;
            int iterations = 0;
            while (pos < end) {
                if (++iterations > MAX_VARINT_BYTES) {
                    throw new IllegalArgumentException("varint too long");
                }
                int b = data[pos++] & 0xFF;
                result |= (long) (b & 0x7F) << shift;
                if ((b & 0x80) == 0) {
                    return result;
                }
                shift += 7;
            }
            throw new IllegalArgumentException("truncated varint");
        }

        byte[] readLenDelim() {
            long lenLong = readVarint();
            if (lenLong < 0 || lenLong > Limits.MAX_PROTO_FIELD_BYTES) {
                throw new IllegalArgumentException(
                        "length-delimited field too large (max " + Limits.MAX_PROTO_FIELD_BYTES + " bytes)");
            }
            int len = (int) lenLong;
            if (pos + len > end) {
                throw new IllegalArgumentException("truncated length-delimited field");
            }
            byte[] result = Arrays.copyOfRange(data, pos, pos + len);
            pos += len;
            return result;
        }

        void skip(int wireType) {
            switch (wireType) {
                case 0 -> readVarint();
                case 1 -> pos += 8;
                case 2 -> readLenDelim();
                case 5 -> pos += 4;
                default -> throw new IllegalArgumentException("unknown wire type: " + wireType);
            }
        }

        ProtoMap readMessage() {
            var fields = new HashMap<Integer, List<Object>>();
            while (pos < end) {
                long tag = readVarint();
                int fieldNum = (int) (tag >>> 3);
                int wireType = (int) (tag & 7);
                if (fieldNum == 0) break;

                Object value = switch (wireType) {
                    case 0 -> readVarint();
                    case 1 -> { pos += 8; yield 0L; }
                    case 2 -> readLenDelim();
                    case 5 -> { pos += 4; yield 0L; }
                    default -> { skip(wireType); yield null; }
                };
                if (value != null) {
                    fields.computeIfAbsent(fieldNum, k -> new ArrayList<>()).add(value);
                }
            }
            return new ProtoMap(fields, decodeDepth);
        }
    }

    // === ProtoMap ===

    public record ProtoMap(Map<Integer, List<Object>> fields, int decodeDepth) {

        public long getVarint(int field) {
            var vals = fields.get(field);
            if (vals == null || vals.isEmpty()) return 0;
            return (vals.getFirst() instanceof Long l) ? l : 0;
        }

        public int getInt(int field) { return (int) getVarint(field); }
        public boolean getBool(int field) { return getVarint(field) != 0; }

        public byte[] getRawBytes(int field) {
            var vals = fields.get(field);
            if (vals == null || vals.isEmpty()) return new byte[0];
            return (vals.getFirst() instanceof byte[] b) ? b : new byte[0];
        }

        public String getString(int field) {
            return new String(getRawBytes(field), StandardCharsets.UTF_8);
        }

        public ProtoMap getMessage(int field) {
            byte[] raw = getRawBytes(field);
            return raw.length > 0
                    ? Proto.decode(raw, decodeDepth + 1)
                    : new ProtoMap(Map.of(), decodeDepth);
        }

        public List<ProtoMap> getRepeatedMessages(int field) {
            var vals = fields.get(field);
            if (vals == null) return List.of();
            var result = new ArrayList<ProtoMap>();
            for (var v : vals) {
                if (v instanceof byte[] b && b.length > 0) {
                    result.add(Proto.decode(b, decodeDepth + 1));
                }
            }
            return result;
        }

        public Map<String, String> getStringMap(int field) {
            var result = new LinkedHashMap<String, String>();
            var vals = fields.get(field);
            if (vals == null) return result;
            for (var v : vals) {
                if (v instanceof byte[] b && b.length > 0) {
                    var entry = Proto.decode(b, decodeDepth + 1);
                    result.put(entry.getString(1), entry.getString(2));
                }
            }
            return result;
        }

        public Map<String, Object> toMap(Map<Integer, FieldDef> schema) {
            var result = new LinkedHashMap<String, Object>();
            for (var entry : schema.entrySet()) {
                int num = entry.getKey();
                var def = entry.getValue();
                if (!fields.containsKey(num)) continue;

                result.put(def.name(), switch (def.type()) {
                    case VARINT -> getVarint(num);
                    case INT32 -> getInt(num);
                    case BOOL -> getBool(num);
                    case STRING -> getString(num);
                    case BYTES -> getRawBytes(num);
                    case MESSAGE -> def.nested() != null
                        ? getMessage(num).toMap(def.nested()) : Map.of();
                    case REPEATED_MESSAGE -> def.nested() != null
                        ? getRepeatedMessages(num).stream()
                            .map(m -> m.toMap(def.nested())).toList()
                        : List.of();
                    case STRING_MAP -> getStringMap(num);
                });
            }
            return result;
        }
    }

    public enum FieldType {
        VARINT, INT32, BOOL, STRING, BYTES, MESSAGE, REPEATED_MESSAGE, STRING_MAP
    }

    public record FieldDef(String name, FieldType type, Map<Integer, FieldDef> nested) {
        public FieldDef(String name, FieldType type) { this(name, type, null); }
    }

    private Proto() {}
}
