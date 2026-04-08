package com.piratetok.live.http;

import com.piratetok.live.Limits;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonTest {

    @Test
    void parseObject_rejectsNonObject() {
        assertThrows(IllegalArgumentException.class, () -> Json.parseObject("[1]"));
    }

    @Test
    void parse_rejectsExcessiveNesting() {
        int depth = Limits.JSON_MAX_NESTING_DEPTH + 1;
        StringBuilder sb = new StringBuilder();
        sb.append("[".repeat(depth));
        sb.append("0");
        sb.append("]".repeat(depth));
        assertThrows(IllegalArgumentException.class, () -> Json.parse(sb.toString()));
    }
}
