package com.piratetok.live;

/**
 * Hard caps for untrusted input from TikTok servers (WSS, HTTP JSON, protobuf payloads).
 */
public final class Limits {

    /** Max size of a reassembled binary WebSocket message (all fragments). */
    public static final int MAX_WSS_FRAME_BYTES = 16 * 1024 * 1024;

    /** Max decompressed size for gzip-wrapped im payloads. */
    public static final int MAX_GZIP_DECOMPRESSED_BYTES = 16 * 1024 * 1024;

    /** Max length of a single protobuf length-delimited field (wire type 2). */
    public static final int MAX_PROTO_FIELD_BYTES = 16 * 1024 * 1024;

    /** Max recursive depth when decoding nested protobuf messages. */
    public static final int MAX_PROTO_NEST_DEPTH = 64;

    public static final int JSON_MAX_NESTING_DEPTH = 200;
    public static final int JSON_MAX_STRING_LENGTH = 50_000_000;
    public static final int JSON_MAX_NUMBER_LENGTH = 1000;

    private Limits() {}
}
