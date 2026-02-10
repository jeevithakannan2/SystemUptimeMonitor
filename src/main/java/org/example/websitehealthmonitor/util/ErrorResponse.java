package org.example.websitehealthmonitor.util;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ErrorResponse {
    public static void sendJsonError(HttpServletResponse resp, int statusCode, String message) throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write("{\"error\":\"" + message.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}");
    }
}
