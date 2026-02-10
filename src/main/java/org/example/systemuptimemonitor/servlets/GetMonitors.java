package org.example.systemuptimemonitor.servlets;

import org.example.systemuptimemonitor.model.Monitor;
import org.example.systemuptimemonitor.model.User;
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

@WebServlet("/monitors")
public class GetMonitors extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(GetMonitors.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = (User) req.getSession().getAttribute("user");
        MonitorService monitorService = new MonitorService();
        ArrayList<Monitor> monitors;
        try {
            monitors = monitorService.getAllMonitorsByOrganization(user.getOrganization());
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to load monitors for org: " + user.getOrganization(), e);
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to load monitors");
            return;
        }

        LOG.fine("Returning " + monitors.size() + " monitors for org: " + user.getOrganization());

        resp.setContentType("application/json");
        PrintWriter pw = resp.getWriter();
        pw.print("{\"monitors\":[");
        for(int i = 0; i < monitors.size(); i++) {
            Monitor monitor = monitors.get(i);
            pw.print("{\"id\":" + monitor.getId()+ ",");
            pw.print("\"name\":\"" + monitor.getName() + "\",");
            pw.print("\"target_url\":\"" + monitor.getTargetUrl() + "\",");
            pw.print("\"check_interval\":" + monitor.getCheckInterval() + ",");
            pw.print("\"created_time\":\"" + new Timestamp(monitor.getCreatedTime()) + "\",");
            pw.print("\"failure_count\":" + monitor.getFailureCount() + ",");
            pw.print("\"organization\":\"" + monitor.getOrganization() + "\",");
            pw.print("\"status_codes\": [");
            ArrayList<Integer> codes = monitor.getStatusCodes();
            if (codes != null) {
                for (int j = 0; j < codes.size(); j++) {
                    pw.print(codes.get(j));
                    if (j < codes.size() - 1) pw.print(",");
                }
            }
            pw.print("],");
            pw.print("\"enabled\":" + monitor.isEnabled());
            pw.print("}");
            if (i < monitors.size() - 1) pw.print(",");
        }
        pw.print("]}");
    }
}
