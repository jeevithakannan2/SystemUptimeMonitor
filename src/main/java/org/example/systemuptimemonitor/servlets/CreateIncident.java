package org.example.systemuptimemonitor.servlets;

import org.example.systemuptimemonitor.dao.StatusCodeDao;
import org.example.systemuptimemonitor.model.MonitorRun;
import org.example.systemuptimemonitor.model.User;
import org.example.systemuptimemonitor.services.IncidentService;
import org.example.systemuptimemonitor.util.DBManager;
import org.example.systemuptimemonitor.util.ErrorResponse;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/create_incident")
public class CreateIncident extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(CreateIncident.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = (User) req.getSession().getAttribute("user");
        IncidentService incidentService = new IncidentService();
        String monitorIdStr = req.getParameter("monitor_id");
        String statusCodeStr = req.getParameter("status_code");

        if (monitorIdStr == null || monitorIdStr.isEmpty() || statusCodeStr == null || statusCodeStr.isEmpty()) {
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "monitor_id and status_code are required");
            return;
        }

        int monitorId;
        int statusCode;
        try {
            monitorId = Integer.parseInt(monitorIdStr);
            statusCode = Integer.parseInt(statusCodeStr);
            if (monitorId < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "monitor_id and status_code not a valid number");
            return;
        }

        // Fetch expected status codes for the monitor
        String expectedCodesStr = "";
        try (Connection conn = DBManager.getConnection(user.getOrganization())) {
            StatusCodeDao statusCodeDao = new StatusCodeDao();
            ArrayList<Integer> codes = statusCodeDao.getStatusCodes(conn, monitorId);
            if (codes != null && !codes.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < codes.size(); i++) {
                    sb.append(codes.get(i));
                    if (i < codes.size() - 1) sb.append(",");
                }
                expectedCodesStr = sb.toString();
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to fetch status codes for monitor " + monitorId, e);
        }

        MonitorRun monitorRun = new MonitorRun(monitorId, System.currentTimeMillis(), 0, statusCode);
        try {
            incidentService.createIncident(monitorRun, expectedCodesStr, user.getOrganization());
            LOG.info("Incident created for monitor_id=" + monitorId + " status_code=" + statusCode);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to create incident for monitor_id=" + monitorId, e);
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error creating an incident");
        }
    }
}
