package org.example.systemuptimemonitor.servlets;

import org.example.systemuptimemonitor.exceptions.MissingMonitorException;
import org.example.systemuptimemonitor.model.User;
import org.example.systemuptimemonitor.services.MonitorService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/delete_monitor")
public class DeleteMonitor extends HttpServlet {
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String idStr = req.getParameter("id");
        if (idStr == null || idStr.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "target_url is mandatory");
            return;
        }
        MonitorService monitorService = new MonitorService();
        try {
            int id = Integer.parseInt(idStr);
            monitorService.deleteMonitor(id);
        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (MissingMonitorException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Specified monitor not found");
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Not a valid monitor id");
        }
    }
}
