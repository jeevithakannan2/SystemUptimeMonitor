package org.example.systemuptimemonitor.servlets;

import org.example.systemuptimemonitor.model.MonitorAudit;
import org.example.systemuptimemonitor.services.MonitorService;
import org.example.systemuptimemonitor.util.ErrorResponse;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/monitor_history")
public class GetMonitorHistory extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(GetMonitorHistory.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String idStr = req.getParameter("id");
        if (idStr == null) {
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing required parameter: id");
            return;
        }

        int monitorId;
        try {
            monitorId = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid ID format");
            return;
        }

        MonitorService monitorService = new MonitorService();
        ArrayList<MonitorAudit> history;
        try {
            history = monitorService.getMonitorHistory(monitorId);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to load history for monitor: " + monitorId, e);
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to load history");
            return;
        }

        resp.setContentType("application/json");
        PrintWriter pw = resp.getWriter();
        pw.print("{\"history\":[");
        for (int i = 0; i < history.size(); i++) {
            MonitorAudit audit = history.get(i);
            pw.print("{\"id\":" + audit.getId() + ",");
            pw.print("\"monitor_id\":" + audit.getMonitorId() + ",");
            pw.print("\"operation\":\"" + audit.getOperation() + "\",");
            pw.print("\"time\":\"" + new Timestamp(audit.getTime()) + "\"");
            pw.print("}");
            if (i < history.size() - 1) pw.print(",");
        }
        pw.print("]}");
    }
}
