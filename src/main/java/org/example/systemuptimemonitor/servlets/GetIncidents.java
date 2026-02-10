package org.example.systemuptimemonitor.servlets;

import org.example.systemuptimemonitor.model.Incident;
import org.example.systemuptimemonitor.model.User;
import org.example.systemuptimemonitor.services.IncidentService;
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

@WebServlet("/incidents")
public class GetIncidents extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(GetIncidents.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = (User) req.getSession().getAttribute("user");
        IncidentService incidentService = new IncidentService();
        ArrayList<Incident> incidents = null;
        try {
            incidents = incidentService.getAllIncidentsByOrganization(user.getOrganization());
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to load incidents for org: " + user.getOrganization(), e);
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to load incidents");
            return;
        }

        resp.setContentType("application/json");
        PrintWriter pw = resp.getWriter();
        pw.print("{\"incidents\":[");
        for(int i = 0; i < incidents.size(); i++) {
            Incident incident = incidents.get(i);
            pw.print("{\"id\":" + incident.getId()+ ",");
            pw.print("\"monitor_id\":" + incident.getMonitorId() + ",");
            pw.print("\"monitor_run_id\":" + incident.getMonitorRunId() + ",");
            pw.print("\"down_time\":\"" + new Timestamp(incident.getDownTime()) + "\",");
            pw.print("\"resolved_time\":" + incident.getResolvedTime() + ",");
            pw.print("\"status_code\":" + incident.getStatusCode() + ",");
            String expectedCodes = incident.getExpectedStatusCodes();
            pw.print("\"expected_status_codes\":");
            if (expectedCodes != null) {
                pw.print("\"" + expectedCodes + "\"");
            } else {
                pw.print("\"\"");
            }
            pw.print(",\"resolved\":" + incident.isResolved());
            pw.print("}");
            if (i < incidents.size() - 1) pw.print(",");
        }
        pw.print("]}");
    }
}
