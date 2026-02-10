package org.example.systemuptimemonitor.util;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses URL-encoded request bodies for PUT and DELETE methods,
 * where Tomcat does not populate req.getParameter() automatically.
 */
public class RequestBodyParser {

    public static Map<String, String> parse(HttpServletRequest req) throws IOException {
        Map<String, String> params = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        String body = sb.toString().trim();
        if (body.isEmpty()) return params;

        for (String pair : body.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                String value = idx < pair.length() - 1
                        ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                        : "";
                params.put(key, value);
            }
        }
        return params;
    }
}
