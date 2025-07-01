package org.example.systemuptimemonitor.servlets;

import org.example.systemuptimemonitor.model.Incident;
import org.example.systemuptimemonitor.model.User;
import org.example.systemuptimemonitor.services.IncidentService;

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

@WebServlet("/incidents")
public class GetIncidents extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = (User) req.getSession().getAttribute("user");
        IncidentService incidentService = new IncidentService();
        ArrayList<Incident> incidents = null;
        try {
            incidents = incidentService.getAllIncidentsByOrganization(user.getOrganization());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        PrintWriter pw = resp.getWriter();
        pw.println("{");
        pw.print("\"incidents\":");
        pw.println("[");
        for(Incident incident: incidents) {
            pw.println("{");
            pw.println("\"id\":" + incident.getId()+ ",");
            pw.println("\"monitor_run_id\":\"" + incident.getMonitorRunId() + ",");
            pw.println("\"down_time\":\"" + new Timestamp(incident.getDownTime()) + "\",");
            pw.println("\"resolved_time\":" + incident.getResolvedTime() + "\",");
            pw.println("\"status_code\":\"" + incident.getStatusCode() + ",");
            pw.println("\"resolved\":" + incident.isResolved() + ",");
            pw.println("},");
        }
        pw.print("]");
        pw.println("}");
    }
}
