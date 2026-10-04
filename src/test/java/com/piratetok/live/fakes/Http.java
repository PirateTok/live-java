package com.piratetok.live.fakes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class Http {

    /** Reads an HTTP request head; header keys lowercased, plus "request-target". {@code null} on EOF. */
    static Map<String, String> readHead(InputStream in) throws IOException {
        var buf = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            buf.write(b);
            String s = buf.toString(StandardCharsets.US_ASCII);
            if (s.endsWith("\r\n\r\n")) {
                String[] lines = s.split("\r\n");
                var head = new HashMap<String, String>();
                head.put("request-target", lines[0].split(" ")[1]);
                for (int i = 1; i < lines.length; i++) {
                    int colon = lines[i].indexOf(':');
                    head.put(lines[i].substring(0, colon).trim().toLowerCase(Locale.ROOT), lines[i].substring(colon + 1).trim());
                }
                return head;
            }
        }
        return null;
    }

    private Http() {}
}
