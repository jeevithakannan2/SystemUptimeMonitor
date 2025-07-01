package org.example.systemuptimemonitor.servlets;

import org.example.systemuptimemonitor.exceptions.MissingMonitorException;
import org.example.systemuptimemonitor.model.Monitor;
import org.example.systemuptimemonitor.model.User;
import org.example.systemuptimemonitor.services.MonitorService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

@WebServlet("/update_monitor")
public class UpdateMonitor extends HttpServlet {
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String monitorIdStr = req.getParameter("monitor_id");
        String name = req.getParameter("name");
        String targetUrl = req.getParameter("target_url");
        String expected_status_codes= req.getParameter("expected_status_codes");
        String check_interval = req.getParameter("check_interval");
        String enabled = req.getParameter("enabled");
        String failureCount = req.getParameter("failure_count");

        if (monitorIdStr == null || monitorIdStr.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "monitor_id must be a valid number");
            return;
        }

        MonitorService monitorService = new MonitorService();
        try {
            int monitorId = Integer.parseInt(monitorIdStr);
            Monitor monitor = monitorService.getMonitor(monitorId);
            if(name != null && !name.isEmpty()) monitor.setName(name);
            if(targetUrl != null && !targetUrl.isEmpty()) monitor.setTargetUrl(targetUrl);
            if(expected_status_codes != null && !expected_status_codes.isEmpty()) {
                String[] codes = expected_status_codes.split(",");
                ArrayList<Integer> statusCodesList = new ArrayList<>();
                for (String code: codes) statusCodesList.add(Integer.parseInt(code));
                monitor.setStatusCodes(statusCodesList);
            }
            if(check_interval != null && !check_interval.isEmpty()) monitor.setCheckInterval(Integer.parseInt(check_interval));
            if(enabled != null && !enabled.isEmpty()) monitor.setEnabled(Boolean.parseBoolean(enabled));
            if(failureCount != null && !failureCount.isEmpty()) monitor.setFailureCount(Integer.parseInt(failureCount));
            monitorService.updateMonitor(monitor);
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        } catch (MissingMonitorException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Monitor target URL not found");
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Not a valid number");
        }
    }
}
