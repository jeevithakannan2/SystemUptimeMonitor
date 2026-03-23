package org.example.systemuptimemonitor.resources;

import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.example.systemuptimemonitor.filter.OperatorAuth;
import org.example.systemuptimemonitor.dao.StatusCodeDao;
import org.example.systemuptimemonitor.exceptions.IncidentAlreadyResolvedException;
import org.example.systemuptimemonitor.exceptions.MissingIncidentException;
import org.example.systemuptimemonitor.model.Incident;
import org.example.systemuptimemonitor.model.MonitorRun;
import org.example.systemuptimemonitor.model.User;
import org.example.systemuptimemonitor.services.IncidentService;
import org.example.systemuptimemonitor.util.DBManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/")
@OperatorAuth
public class IncidentResource {
    private static final Logger LOG = Logger.getLogger(IncidentResource.class.getName());

    @GET
    @Path("/incidents")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getIncidents(@Context ContainerRequestContext crc) {
        User user = (User) crc.getProperty("user");
        IncidentService incidentService = new IncidentService();
        ArrayList<Incident> incidents;
        try {
            incidents = incidentService.getAllIncidentsByOrganization(user.getOrganization());
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to load incidents for org: " + user.getOrganization(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Failed to load incidents\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"incidents\":[");
        for (int i = 0; i < incidents.size(); i++) {
            Incident incident = incidents.get(i);
            sb.append("{\"id\":").append(incident.getId()).append(",");
            sb.append("\"monitor_id\":").append(incident.getMonitorId()).append(",");
            sb.append("\"monitor_run_id\":").append(incident.getMonitorRunId()).append(",");
            sb.append("\"down_time\":\"").append(new Timestamp(incident.getDownTime())).append("\",");
            sb.append("\"resolved_time\":").append(incident.getResolvedTime()).append(",");
            sb.append("\"status_code\":").append(incident.getStatusCode()).append(",");
            String expectedCodes = incident.getExpectedStatusCodes();
            sb.append("\"expected_status_codes\":");
            if (expectedCodes != null) {
                sb.append("\"").append(expectedCodes).append("\"");
            } else {
                sb.append("\"\"");
            }
            sb.append(",\"resolved\":").append(incident.isResolved());
            sb.append("}");
            if (i < incidents.size() - 1) sb.append(",");
        }
        sb.append("]}");
        return Response.ok(sb.toString(), MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/create_incident")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createIncident(@Context ContainerRequestContext crc,
                                   @FormParam("monitor_id") String monitorIdStr,
                                   @FormParam("status_code") String statusCodeStr) {
        User user = (User) crc.getProperty("user");

        if (monitorIdStr == null || monitorIdStr.isEmpty() || statusCodeStr == null || statusCodeStr.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"monitor_id and status_code are required\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }

        int monitorId;
        int statusCode;
        try {
            monitorId = Integer.parseInt(monitorIdStr);
            statusCode = Integer.parseInt(statusCodeStr);
            if (monitorId < 0 || statusCode < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"monitor_id and status_code not a valid number\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }

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
        IncidentService incidentService = new IncidentService();
        try {
            incidentService.createIncident(monitorRun, expectedCodesStr, user.getOrganization());
            LOG.info("Incident created for monitor_id=" + monitorId + " status_code=" + statusCode);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to create incident for monitor_id=" + monitorId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Error creating an incident\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }

        return Response.ok().build();
    }

    @PUT
    @Path("/resolve_incident")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response resolveIncident(@Context ContainerRequestContext crc,
                                    @FormParam("incident_id") String incidentIdStr,
                                    @FormParam("notes") String notes) {
        if (incidentIdStr == null || incidentIdStr.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"incident_id cannot be empty\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }

        int incidentId;
        try {
            incidentId = Integer.parseInt(incidentIdStr);
            if (incidentId < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"incident_id must be a positive number\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }

        if (notes == null) notes = "";
        if (notes.length() >= 256) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Notes cannot be more than 256 characters\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }

        User user = (User) crc.getProperty("user");
        IncidentService incidentService = new IncidentService();

        try {
            incidentService.resolveIncident(incidentId, notes, user.getOrganization());
            LOG.info("Incident resolved: id=" + incidentId);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to resolve incident: id=" + incidentId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Error when resolving the incident\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (MissingIncidentException e) {
            LOG.warning("Incident not found for resolution: id=" + incidentId);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Specified incident not found\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (IncidentAlreadyResolvedException e) {
            LOG.warning("Incident already resolved: id=" + incidentId);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Incident already resolved\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }

        return Response.ok().build();
    }
}
