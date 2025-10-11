package unitbv.devops.util;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class HttpUtils {
    private HttpUtils() {}

    public static String readBody(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static void sendText(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    public static void sendEmpty(HttpExchange ex, int status) throws IOException {
        ex.sendResponseHeaders(status, -1);
        ex.close();
    }

    /** Parse query string into a multimap. */
    public static Map<String, List<String>> queryParams(HttpExchange ex) {
        String rawQuery = ex.getRequestURI().getRawQuery();
        Map<String, List<String>> map = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) return map;

        for (String pair : rawQuery.split("&")) {
            if (pair.isBlank()) continue;
            String[] kv = pair.split("=", 2);
            String k = urlDecode(kv[0]);
            String v = kv.length > 1 ? urlDecode(kv[1]) : "";
            map.computeIfAbsent(k, _k -> new ArrayList<>()).add(v);
        }
        return map;
    }

    public static Optional<String> firstQueryParam(HttpExchange ex, String key) {
        List<String> vals = queryParams(ex).get(key);
        if (vals == null || vals.isEmpty()) return Optional.empty();
        return Optional.ofNullable(vals.get(0));
    }

    private static String urlDecode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}
