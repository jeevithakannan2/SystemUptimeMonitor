package org.example.systemuptimemonitor.servlets;

import org.example.systemuptimemonitor.model.Monitor;
import org.example.systemuptimemonitor.model.User;
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

@WebServlet("/monitors")
public class GetMonitors extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = (User) req.getSession().getAttribute("user");
        MonitorService monitorService = new MonitorService();
        ArrayList<Monitor> monitors;
        try {
            monitors = monitorService.getAllMonitorsByOrganization(user.getOrganization());
        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        PrintWriter pw = resp.getWriter();
        pw.println("{");
        pw.print("\"monitors:\"");
        pw.println("[");
        for(Monitor monitor: monitors) {
            pw.println("{");
            pw.println("\"id\":" + monitor.getId()+ ",");
            pw.println("\"name\":\"" + monitor.getName() + "\",");
            pw.println("\"target_url\":\"" + monitor.getTargetUrl() + "\",");
            pw.println("\"check_interval\":" + monitor.getCheckInterval() + ",");
            pw.println("\"created_time\":\"" + new Timestamp(monitor.getCreatedTime()) + "\",");
            pw.println("\"failure_count\":" + monitor.getFailureCount() + ",");
            pw.println("\"organization\":\"" + monitor.getOrganization() + "\",");
            pw.println("\"enabled\":" + monitor.isEnabled() + ",");
            pw.println("},");
        }
        pw.print("]");
        pw.println("}");
    }
}
