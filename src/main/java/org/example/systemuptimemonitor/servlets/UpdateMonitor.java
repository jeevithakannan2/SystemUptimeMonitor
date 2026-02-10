package org.example.systemuptimemonitor.servlets;

import org.example.systemuptimemonitor.exceptions.MissingMonitorException;
import org.example.systemuptimemonitor.model.Monitor;
import org.example.systemuptimemonitor.services.MonitorService;
import org.example.systemuptimemonitor.util.ErrorResponse;
import org.example.systemuptimemonitor.util.RequestBodyParser;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/update_monitor")
public class UpdateMonitor extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(UpdateMonitor.class.getName());

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, String> params = RequestBodyParser.parse(req);
        String monitorIdStr = params.get("monitor_id");
        String name = params.get("name");
        String targetUrl = params.get("target_url");
        String expected_status_codes = params.get("expected_status_codes");
        String check_interval = params.get("check_interval");
        String enabled = params.get("enabled");
        String failureCount = params.get("failure_count");

        if (monitorIdStr == null || monitorIdStr.isEmpty()) {
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "monitor_id must be a valid number");
            return;
        }

        MonitorService monitorService = new MonitorService();
        try {
            int monitorId = Integer.parseInt(monitorIdStr);
            Monitor monitor = monitorService.getMonitor(monitorId);
            if (name != null && !name.isEmpty()) monitor.setName(name);
            if (targetUrl != null && !targetUrl.isEmpty()) monitor.setTargetUrl(targetUrl);
            if (expected_status_codes != null && !expected_status_codes.isEmpty()) {
                String[] codes = expected_status_codes.split(",");
                ArrayList<Integer> statusCodesList = new ArrayList<>();
                for (String code : codes) statusCodesList.add(Integer.parseInt(code));
                monitor.setStatusCodes(statusCodesList);
            }
            if (check_interval != null && !check_interval.isEmpty())
                monitor.setCheckInterval(Integer.parseInt(check_interval));
            if (enabled != null && !enabled.isEmpty()) monitor.setEnabled(Boolean.parseBoolean(enabled));
            if (failureCount != null && !failureCount.isEmpty())
                monitor.setFailureCount(Integer.parseInt(failureCount));
            monitorService.updateMonitor(monitor);
            LOG.info("Monitor updated: id=" + monitorId);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to update monitor: " + monitorIdStr, e);
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
        } catch (MissingMonitorException e) {
            LOG.warning("Monitor not found for update: " + monitorIdStr);
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Monitor target URL not found");
        } catch (NumberFormatException e) {
            LOG.warning("Invalid number in update monitor request: " + monitorIdStr);
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Not a valid number");
        }
    }
}
