package org.example.systemuptimemonitor.servlets;

import org.example.systemuptimemonitor.exceptions.IncidentAlreadyResolvedException;
import org.example.systemuptimemonitor.exceptions.MissingIncidentException;
import org.example.systemuptimemonitor.services.IncidentService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/resolve_incident")
public class ResolveIncident extends HttpServlet {
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String incidentIdStr = req.getParameter("incident_id");
        String notes = req.getParameter("notes");
        if (incidentIdStr == null || incidentIdStr.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "incident_id cannot be empty");
            return;
        }
        if (notes == null) notes = "";
        if (notes.length() >= 256) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Notes cannot be more than 256 words");
            return;
        }

        int incidentId;
        try {
            incidentId = Integer.parseInt(incidentIdStr);
            if (incidentId < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "incident_id must be a positive number");
            return;
        }

        IncidentService incidentService = new IncidentService();

        try {
            incidentService.resolveIncident(incidentId, notes);
        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error when resolving the incident");
        } catch (MissingIncidentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Specified incident not found");
        } catch (IncidentAlreadyResolvedException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incident already resolved");
        }
    }
}
