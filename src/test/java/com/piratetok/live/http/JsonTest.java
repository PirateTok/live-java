package com.piratetok.live.http;

import com.piratetok.live.Limits;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonTest {

    @Test
    void parseObject_nestedAndPrimitives() {
        Map<String, Object> m = Json.parseObject("""
                {"statusCode":0,"data":{"user":{"roomId":"7123456789"},"n":42}}""");
        assertEquals(0, ((Number) m.get("statusCode")).intValue());
        @SuppressWarnings("unchecked")
        var data = (Map<String, Object>) m.get("data");
        @SuppressWarnings("unchecked")
        var user = (Map<String, Object>) data.get("user");
        assertEquals("7123456789", user.get("roomId"));
        assertEquals(42, ((Number) data.get("n")).intValue());
    }

    @Test
    void parseObject_stringEscapes() {
        Map<String, Object> m = Json.parseObject("{\"t\":\"line1\\nline2\\t\\\"\"}");
        assertEquals("line1\nline2\t\"", m.get("t"));
    }

    @Test
    void parse_arrayAndNullAndBool() {
        Object v = Json.parse("[true,false,null,3.5]");
        assertInstanceOf(List.class, v);
        @SuppressWarnings("unchecked")
        var list = (List<Object>) v;
        assertEquals(Boolean.TRUE, list.get(0));
        assertEquals(Boolean.FALSE, list.get(1));
        assertNull(list.get(2));
        assertEquals(3.5, ((Number) list.get(3)).doubleValue());
    }

    @Test
    void parseObject_rejectsNonObject() {
        assertThrows(IllegalArgumentException.class, () -> Json.parseObject("[1]"));
    }

    @Test
    void parseObject_emptyObject() {
        Map<String, Object> m = Json.parseObject("{}");
        assertTrue(m.isEmpty());
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
