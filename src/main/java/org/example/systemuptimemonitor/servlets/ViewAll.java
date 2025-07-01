package org.example.systemuptimemonitor.servlets;

import org.example.systemuptimemonitor.model.Incident;
import org.example.systemuptimemonitor.model.Monitor;
import org.example.systemuptimemonitor.services.IncidentService;
import org.example.systemuptimemonitor.services.MonitorService;

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

@WebServlet("/status")
public class ViewAll extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        MonitorService monitorService = new MonitorService();
        IncidentService incidentService = new IncidentService();

        ArrayList<Monitor> monitors;
        try {
            monitors = monitorService.getAllMonitors();
        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        resp.setContentType("application/json");
        PrintWriter pw = resp.getWriter();
        pw.println("{");
        pw.print("\"monitors:\"");
        pw.println("[");
        for(int i = 0; i < monitors.size(); i++) {
            Monitor monitor = monitors.get(i);
            ArrayList<Incident> incidents = null;
            try {
                incidents = incidentService.getIncidentsByMonitor(monitor.getId());
            } catch (SQLException e) {
                e.printStackTrace();
            }

            pw.println("{");
            pw.println("\"id\":" + monitor.getId()+ ",");
            pw.println("\"name\":\"" + monitor.getName() + "\",");
            pw.println("\"target_url\":\"" + monitor.getTargetUrl() + "\",");
            pw.println("\"check_interval\":" + monitor.getCheckInterval() + ",");
            pw.println("\"created_time\":\"" + new Timestamp(monitor.getCreatedTime()) + "\",");
            pw.println("\"failure_count\":" + monitor.getFailureCount() + ",");
            pw.println("\"organization\":\"" + monitor.getOrganization() + "\",");
            pw.println("\"enabled\":" + monitor.isEnabled() + ",");
            pw.print("\"incidents\":");
            pw.println("[");

            long created = monitor.getCreatedTime();
            long down = 0;
            if (incidents != null) {
                for (Incident incident : incidents) {
                    down += incident.getResolvedTime() - incident.getDownTime();
                    pw.println("\"id\":" + incident.getId() + ",");
                    pw.println("\"monitor_run_id\":" + incident.getMonitorRunId() + ",");
                    pw.println("\"down_time\":\"" + new Timestamp(incident.getDownTime()) + "\",");
                    pw.println("\"resolved_time\":\"" + new Timestamp(incident.getResolvedTime()) + "\",");
                    pw.println("\"status_code\":" + incident.getStatusCode());
                }
            }
            pw.println("],");
            down = Math.abs(down);
            long uptime = created - down;
            pw.println("\"uptime%\":" + ((uptime/created) * 100));
            if (i == monitors.size() - 1)
                pw.println("}");
            else
                pw.println("},");
        }
        pw.print("]");
        pw.println("}");
    }
}
