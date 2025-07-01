package org.example.systemuptimemonitor.servlets;

import org.example.systemuptimemonitor.exceptions.MonitorAlreadyExistsException;
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

@WebServlet("/create_monitor")
public class CreateMonitor extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = (User) req.getSession().getAttribute("user");
        String name = req.getParameter("name");
        String targetUrl = req.getParameter("target_url");
        String expected_status_codes= req.getParameter("expected_status_codes");
        String check_interval = req.getParameter("check_interval");
        String enabled = req.getParameter("enabled");
        String failureCount = req.getParameter("failure_count");

        if (name == null || name.isEmpty() || targetUrl == null || targetUrl.isEmpty() || expected_status_codes == null || expected_status_codes.isEmpty() || check_interval == null || check_interval.isEmpty() || enabled == null || enabled.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        int checkInterval = Integer.parseInt(check_interval);
        boolean enabled1 = Boolean.parseBoolean(req.getParameter("enabled"));

        int failureCount1;
        if (failureCount == null)
            failureCount1 = 3;
        else
            failureCount1 =  Integer.parseInt(req.getParameter("failure_count"));

        String[] codes = expected_status_codes.split(",");
        ArrayList<Integer> statusCodesList = new ArrayList<>();
        for (String code: codes) statusCodesList.add(Integer.parseInt(code));

        Monitor monitor = new Monitor(name, targetUrl, checkInterval, System.currentTimeMillis(), user.getId(), failureCount1, user.getOrganization(), enabled1);
        monitor.setStatusCodes(statusCodesList);

        MonitorService monitorService = new MonitorService();
        try {
            monitorService.createMonitor(monitor);
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        } catch (MonitorAlreadyExistsException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Monitor target URL already exists");
        }
    }
}
