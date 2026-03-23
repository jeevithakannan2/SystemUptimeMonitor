package org.example.systemuptimemonitor.resources;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.example.systemuptimemonitor.model.*;
import org.example.systemuptimemonitor.services.*;
import org.example.systemuptimemonitor.util.SchemaManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.*;

@Path("/")
public class PublicResource {

    private static final Logger logger = Logger.getLogger(PublicResource.class.getName());

    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatus(@QueryParam("org") String org) {
        if (org == null || org.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Organization query parameter 'org' is required\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        try {
            if (!SchemaManager.organizationExists(org)) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Organization not found\"}")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }

            List<Monitor> monitors = new MonitorService().getPublicMonitorsByOrganization(org);
            IncidentService incidentService = new IncidentService();

            StringBuilder json = new StringBuilder();
            json.append("{\"organization\":\"").append(org).append("\",\"monitors\":[");

            for (int i = 0; i < monitors.size(); i++) {
                Monitor monitor = monitors.get(i);
                ArrayList<Incident> incidents = incidentService.getIncidentsByMonitor(monitor.getId(), org);

                long created = monitor.getCreatedTime();
                long down = 0;
                for (Incident incident : incidents) {
                    long endTime = incident.isResolved() ? incident.getResolvedTime() : System.currentTimeMillis();
                    down += endTime - incident.getDownTime();
                }
                long total = System.currentTimeMillis() - created;
                double uptimePct = total > 0 ? ((double) (total - down) / total) * 100.0 : 100.0;

                json.append("{\"id\":").append(monitor.getId())
                    .append(",\"name\":\"").append(monitor.getName()).append("\"")
                    .append(",\"target_url\":\"").append(monitor.getTargetUrl()).append("\"")
                    .append(",\"check_interval\":").append(monitor.getCheckInterval())
                    .append(",\"created_time\":\"").append(new Timestamp(monitor.getCreatedTime())).append("\"")
                    .append(",\"failure_count\":").append(monitor.getFailureCount())
                    .append(",\"organization\":\"").append(monitor.getOrganization()).append("\"")
                    .append(",\"enabled\":").append(monitor.isEnabled())
                    .append(",\"incidents\":[");

                for (int j = 0; j < incidents.size(); j++) {
                    Incident incident = incidents.get(j);
                    json.append("{\"id\":").append(incident.getId())
                        .append(",\"monitor_run_id\":").append(incident.getMonitorRunId())
                        .append(",\"down_time\":\"").append(new Timestamp(incident.getDownTime())).append("\"")
                        .append(",\"resolved_time\":").append(incident.isResolved() ? "\"" + new Timestamp(incident.getResolvedTime()) + "\"" : "null")
                        .append(",\"status_code\":").append(incident.getStatusCode())
                        .append("}");
                    if (j < incidents.size() - 1) {
                        json.append(",");
                    }
                }

                json.append("],\"uptime\":").append(String.format("%.2f", uptimePct)).append("}");
                if (i < monitors.size() - 1) {
                    json.append(",");
                }
            }

            json.append("]}");

            return Response.ok(json.toString(), MediaType.APPLICATION_JSON).build();

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error while fetching status", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Internal server error\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    @GET
    @Path("/organizations")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrganizations() {
        try {
            List<String> organizations = new OrganizationService().getAllOrganizations();

            StringBuilder json = new StringBuilder();
            json.append("{\"organizations\":[");
            for (int i = 0; i < organizations.size(); i++) {
                json.append("\"").append(organizations.get(i)).append("\"");
                if (i < organizations.size() - 1) {
                    json.append(",");
                }
            }
            json.append("]}");

            return Response.ok(json.toString(), MediaType.APPLICATION_JSON).build();

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error while fetching organizations", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Internal server error\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
}
