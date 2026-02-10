package org.example.systemuptimemonitor.servlets;

import org.example.systemuptimemonitor.exceptions.IncidentAlreadyResolvedException;
import org.example.systemuptimemonitor.exceptions.MissingIncidentException;
import org.example.systemuptimemonitor.services.IncidentService;
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

@WebServlet("/resolve_incident")
public class ResolveIncident extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(ResolveIncident.class.getName());

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, String> params = RequestBodyParser.parse(req);
        String incidentIdStr = params.get("incident_id");
        String notes = params.get("notes");
        if (incidentIdStr == null || incidentIdStr.isEmpty()) {
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "incident_id cannot be empty");
            return;
        }
        if (notes == null) notes = "";
        if (notes.length() >= 256) {
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Notes cannot be more than 256 characters");
            return;
        }

        int incidentId;
        try {
            incidentId = Integer.parseInt(incidentIdStr);
            if (incidentId < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "incident_id must be a positive number");
            return;
        }

        IncidentService incidentService = new IncidentService();

        try {
            incidentService.resolveIncident(incidentId, notes);
            LOG.info("Incident resolved: id=" + incidentId);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to resolve incident: id=" + incidentId, e);
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error when resolving the incident");
        } catch (MissingIncidentException e) {
            LOG.warning("Incident not found for resolution: id=" + incidentId);
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Specified incident not found");
        } catch (IncidentAlreadyResolvedException e) {
            LOG.warning("Incident already resolved: id=" + incidentId);
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Incident already resolved");
        }
    }
}
