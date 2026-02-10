package org.example.systemuptimemonitor.servlets;

import org.example.systemuptimemonitor.exceptions.MissingMonitorException;
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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/delete_monitor")
public class DeleteMonitor extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(DeleteMonitor.class.getName());

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, String> params = RequestBodyParser.parse(req);
        String idStr = params.get("id");
        if (idStr == null || idStr.isEmpty()) {
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Monitor ID is required");
            return;
        }
        MonitorService monitorService = new MonitorService();
        try {
            int id = Integer.parseInt(idStr);
            monitorService.deleteMonitor(id);
            LOG.info("Monitor deleted: id=" + id);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to delete monitor: " + idStr, e);
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
        } catch (MissingMonitorException e) {
            LOG.warning("Monitor not found for deletion: " + idStr);
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Specified monitor not found");
        } catch (NumberFormatException e) {
            LOG.warning("Invalid monitor id for deletion: " + idStr);
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Not a valid monitor id");
        }
    }
}
