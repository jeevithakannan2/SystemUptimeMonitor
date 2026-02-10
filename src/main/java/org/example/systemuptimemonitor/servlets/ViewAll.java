package org.example.systemuptimemonitor.servlets;

import org.example.systemuptimemonitor.model.Incident;
import org.example.systemuptimemonitor.model.Monitor;
import org.example.systemuptimemonitor.services.IncidentService;
import org.example.systemuptimemonitor.services.MonitorService;
import org.example.systemuptimemonitor.util.ErrorResponse;
import org.example.systemuptimemonitor.util.SchemaManager;

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

@WebServlet("/status")
public class ViewAll extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(ViewAll.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String org = req.getParameter("org");

        if (org == null || org.isEmpty()) {
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing required parameter: org");
            return;
        }

        // Validate that the org exists
        try {
            if (!SchemaManager.organizationExists(org)) {
                ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "Organization not found");
                return;
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to check organization: " + org, e);
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
            return;
        }

        MonitorService monitorService = new MonitorService();
        IncidentService incidentService = new IncidentService();

        ArrayList<Monitor> monitors;
        try {
            monitors = monitorService.getAllMonitorsByOrganization(org);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to load monitors for org: " + org, e);
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to load status");
            return;
        }

        resp.setContentType("application/json");
        PrintWriter pw = resp.getWriter();
        pw.print("{\"organization\":\"" + org + "\",\"monitors\":[");
        for (int i = 0; i < monitors.size(); i++) {
            Monitor monitor = monitors.get(i);
            ArrayList<Incident> incidents = null;
            try {
                incidents = incidentService.getIncidentsByMonitor(monitor.getId(), org);
            } catch (SQLException e) {
                LOG.log(Level.WARNING, "Failed to load incidents for monitor id=" + monitor.getId(), e);
            }

            pw.print("{\"id\":" + monitor.getId() + ",");
            pw.print("\"name\":\"" + monitor.getName() + "\",");
            pw.print("\"target_url\":\"" + monitor.getTargetUrl() + "\",");
            pw.print("\"check_interval\":" + monitor.getCheckInterval() + ",");
            pw.print("\"created_time\":\"" + new Timestamp(monitor.getCreatedTime()) + "\",");
            pw.print("\"failure_count\":" + monitor.getFailureCount() + ",");
            pw.print("\"organization\":\"" + monitor.getOrganization() + "\",");
            pw.print("\"enabled\":" + monitor.isEnabled() + ",");
            pw.print("\"incidents\":[");

            long created = monitor.getCreatedTime();
            long down = 0;
            if (incidents != null) {
                for (int j = 0; j < incidents.size(); j++) {
                    Incident incident = incidents.get(j);
                    long endTime = incident.isResolved() ? incident.getResolvedTime() : System.currentTimeMillis();
                    down += endTime - incident.getDownTime();
                    pw.print("{\"id\":" + incident.getId() + ",");
                    pw.print("\"monitor_run_id\":" + incident.getMonitorRunId() + ",");
                    pw.print("\"down_time\":\"" + new Timestamp(incident.getDownTime()) + "\",");
                    pw.print("\"resolved_time\":\"" + new Timestamp(incident.getResolvedTime()) + "\",");
                    pw.print("\"status_code\":" + incident.getStatusCode() + "}");
                    if (j < incidents.size() - 1) pw.print(",");
                }
            }
            pw.print("],");
            long total = System.currentTimeMillis() - created;
            double uptimePct = total > 0 ? ((double) (total - down) / total) * 100.0 : 100.0;
            pw.print("\"uptime\":" + String.format("%.2f", uptimePct) + "}");
            if (i < monitors.size() - 1) pw.print(",");
        }
        pw.print("]}");
    }
}
