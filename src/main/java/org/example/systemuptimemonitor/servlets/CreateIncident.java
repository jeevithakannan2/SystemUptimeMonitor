package org.example.systemuptimemonitor.servlets;

import org.example.systemuptimemonitor.model.MonitorRun;
import org.example.systemuptimemonitor.services.IncidentService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/create_incident")
public class CreateIncident extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        IncidentService incidentService = new IncidentService();
        String monitorIdStr = req.getParameter("monitor_id");
        String statusCodeStr = req.getParameter("status_code");

        if (monitorIdStr == null || monitorIdStr.isEmpty() || statusCodeStr == null || statusCodeStr.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "monitor_id and status_code should be empty");
            return;
        }

        int monitorId;
        int statusCode;
        try {
            monitorId = Integer.parseInt(monitorIdStr);
            statusCode = Integer.parseInt(statusCodeStr);
            if (monitorId < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "monitor_id and status_code not a valid number");
            return;
        }

        MonitorRun monitorRun = new MonitorRun(monitorId, System.currentTimeMillis(), 0, statusCode);
        try {
            incidentService.createIncident(monitorRun);
        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error creating an incident");
        }
    }
}
