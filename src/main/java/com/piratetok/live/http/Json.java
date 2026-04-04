package com.piratetok.live.http;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piratetok.live.Limits;

import java.util.Map;

public final class Json {

    private static final ObjectMapper MAPPER = new ObjectMapper(
            JsonFactory.builder()
                    .streamReadConstraints(StreamReadConstraints.builder()
                            .maxNestingDepth(Limits.JSON_MAX_NESTING_DEPTH)
                            .maxStringLength(Limits.JSON_MAX_STRING_LENGTH)
                            .maxNumberLength(Limits.JSON_MAX_NUMBER_LENGTH)
                            .build())
                    .build())
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    public static Object parse(String s) {
        try {
            return MAPPER.readValue(s, Object.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String s) {
        Object v = parse(s);
        if (v instanceof Map<?, ?> m) return (Map<String, Object>) m;
        throw new IllegalArgumentException("expected JSON object");
    }

    private Json() {}
}
