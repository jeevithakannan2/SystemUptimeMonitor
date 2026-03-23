package org.example.systemuptimemonitor.resources;

import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.example.systemuptimemonitor.filter.OperatorAuth;
import org.example.systemuptimemonitor.model.Monitor;
import org.example.systemuptimemonitor.model.MonitorAudit;
import org.example.systemuptimemonitor.model.User;
import org.example.systemuptimemonitor.exceptions.MissingMonitorException;
import org.example.systemuptimemonitor.exceptions.MonitorAlreadyExistsException;
import org.example.systemuptimemonitor.services.MonitorService;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/")
@OperatorAuth
public class MonitorResource {

    private static final Logger LOG = Logger.getLogger(MonitorResource.class.getName());

    @Context
    private ContainerRequestContext crc;

    // ── GET /monitors ──────────────────────────────────────────────────────────

    @GET
    @Path("/monitors")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMonitors() {
        User user = (User) crc.getProperty("user");
        try {
            ArrayList<Monitor> monitors = new MonitorService().getAllMonitorsByOrganization(user.getOrganization());
            StringBuilder json = new StringBuilder("{\"monitors\":[");
            for (int i = 0; i < monitors.size(); i++) {
                Monitor m = monitors.get(i);
                if (i > 0) json.append(",");
                json.append("{");
                json.append("\"id\":").append(m.getId()).append(",");
                json.append("\"name\":\"").append(escapeJson(m.getName())).append("\",");
                json.append("\"target_url\":\"").append(escapeJson(m.getTargetUrl())).append("\",");
                json.append("\"check_interval\":").append(m.getCheckInterval()).append(",");
                json.append("\"created_time\":\"").append(new Timestamp(m.getCreatedTime()).toString()).append("\",");
                json.append("\"failure_count\":").append(m.getFailureCount()).append(",");
                json.append("\"organization\":\"").append(escapeJson(m.getOrganization())).append("\",");
                json.append("\"status_codes\":[");
                ArrayList<Integer> codes = m.getStatusCodes();
                if (codes != null) {
                    for (int j = 0; j < codes.size(); j++) {
                        if (j > 0) json.append(",");
                        json.append(codes.get(j));
                    }
                }
                json.append("],");
                json.append("\"enabled\":").append(m.isEnabled()).append(",");
                json.append("\"is_public\":").append(m.isPublic());
                json.append("}");
            }
            json.append("]}");
            return Response.ok(json.toString(), MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error fetching monitors", e);
            return errorResponse(500, "Internal server error");
        }
    }

    // ── POST /create_monitor ───────────────────────────────────────────────────

    @POST
    @Path("/create_monitor")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createMonitor(
            @FormParam("name") String name,
            @FormParam("target_url") String targetUrl,
            @FormParam("expected_status_codes") String expectedStatusCodes,
            @FormParam("check_interval") String checkInterval,
            @FormParam("enabled") String enabled,
            @FormParam("failure_count") String failureCount,
            @FormParam("is_public") String isPublic) {

        User user = (User) crc.getProperty("user");

        if (isNullOrEmpty(name) || isNullOrEmpty(targetUrl) || isNullOrEmpty(expectedStatusCodes)
                || isNullOrEmpty(checkInterval) || isNullOrEmpty(enabled)) {
            return errorResponse(400, "Missing required fields");
        }

        int checkIntervalInt;
        try {
            checkIntervalInt = Integer.parseInt(checkInterval);
        } catch (NumberFormatException e) {
            return errorResponse(400, "Invalid check_interval");
        }

        boolean enabled1 = Boolean.parseBoolean(enabled);

        int failureCountInt = 3;
        if (!isNullOrEmpty(failureCount)) {
            try {
                failureCountInt = Integer.parseInt(failureCount);
            } catch (NumberFormatException e) {
                return errorResponse(400, "Invalid failure_count");
            }
        }

        ArrayList<Integer> statusCodesList = new ArrayList<>();
        try {
            String[] parts = expectedStatusCodes.split(",");
            for (String part : parts) {
                statusCodesList.add(Integer.parseInt(part.trim()));
            }
        } catch (NumberFormatException e) {
            return errorResponse(400, "Invalid expected_status_codes");
        }

        Monitor monitor = new Monitor(name, targetUrl, checkIntervalInt,
                System.currentTimeMillis(), user.getId(), failureCountInt,
                user.getOrganization(), enabled1);
        monitor.setStatusCodes(statusCodesList);
        monitor.setPublic(isPublic != null ? Boolean.parseBoolean(isPublic) : false);

        try {
            new MonitorService().createMonitor(monitor);
            return Response.ok("{\"message\":\"Monitor created\"}", MediaType.APPLICATION_JSON).build();
        } catch (MonitorAlreadyExistsException e) {
            return errorResponse(400, "Monitor already exists");
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error creating monitor", e);
            return errorResponse(500, "Internal server error");
        }
    }

    // ── PUT /update_monitor ────────────────────────────────────────────────────

    @PUT
    @Path("/update_monitor")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateMonitor(
            @FormParam("monitor_id") String monitorIdParam,
            @FormParam("name") String name,
            @FormParam("target_url") String targetUrl,
            @FormParam("expected_status_codes") String expectedStatusCodes,
            @FormParam("check_interval") String checkInterval,
            @FormParam("enabled") String enabled,
            @FormParam("failure_count") String failureCount,
            @FormParam("is_public") String isPublic) {

        User user = (User) crc.getProperty("user");

        if (isNullOrEmpty(monitorIdParam)) {
            return errorResponse(400, "Missing monitor_id");
        }

        int monitorId;
        try {
            monitorId = Integer.parseInt(monitorIdParam);
        } catch (NumberFormatException e) {
            return errorResponse(400, "Invalid monitor_id");
        }

        MonitorService monitorService = new MonitorService();
        try {
            Monitor monitor = monitorService.getMonitor(monitorId, user.getOrganization());

            if (!isNullOrEmpty(name)) {
                monitor.setName(name);
            }
            if (!isNullOrEmpty(targetUrl)) {
                monitor.setTargetUrl(targetUrl);
            }
            if (!isNullOrEmpty(checkInterval)) {
                monitor.setCheckInterval(Integer.parseInt(checkInterval));
            }
            if (!isNullOrEmpty(enabled)) {
                monitor.setEnabled(Boolean.parseBoolean(enabled));
            }
            if (!isNullOrEmpty(failureCount)) {
                monitor.setFailureCount(Integer.parseInt(failureCount));
            }
            if (!isNullOrEmpty(expectedStatusCodes)) {
                ArrayList<Integer> statusCodesList = new ArrayList<>();
                String[] parts = expectedStatusCodes.split(",");
                for (String part : parts) {
                    statusCodesList.add(Integer.parseInt(part.trim()));
                }
                monitor.setStatusCodes(statusCodesList);
            }
            if (!isNullOrEmpty(isPublic)) {
                monitor.setPublic(Boolean.parseBoolean(isPublic));
            }

            monitorService.updateMonitor(monitor);
            return Response.ok("{\"message\":\"Monitor updated\"}", MediaType.APPLICATION_JSON).build();
        } catch (MissingMonitorException e) {
            return errorResponse(400, "Monitor not found");
        } catch (NumberFormatException e) {
            return errorResponse(400, "Invalid numeric parameter");
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error updating monitor", e);
            return errorResponse(500, "Internal server error");
        }
    }

    // ── DELETE /delete_monitor ─────────────────────────────────────────────────

    @DELETE
    @Path("/delete_monitor")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteMonitor(@FormParam("id") String idParam) {
        User user = (User) crc.getProperty("user");

        if (isNullOrEmpty(idParam)) {
            return errorResponse(400, "Missing id");
        }

        int id;
        try {
            id = Integer.parseInt(idParam);
        } catch (NumberFormatException e) {
            return errorResponse(400, "Invalid id");
        }

        try {
            new MonitorService().deleteMonitor(id, user.getOrganization());
            return Response.ok("{\"message\":\"Monitor deleted\"}", MediaType.APPLICATION_JSON).build();
        } catch (MissingMonitorException e) {
            return errorResponse(400, "Monitor not found");
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error deleting monitor", e);
            return errorResponse(500, "Internal server error");
        }
    }

    // ── GET /monitor_history ───────────────────────────────────────────────────

    @GET
    @Path("/monitor_history")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMonitorHistory(@QueryParam("id") String idParam) {
        User user = (User) crc.getProperty("user");

        if (isNullOrEmpty(idParam)) {
            return errorResponse(400, "Missing id");
        }

        int monitorId;
        try {
            monitorId = Integer.parseInt(idParam);
        } catch (NumberFormatException e) {
            return errorResponse(400, "Invalid id");
        }

        try {
            ArrayList<MonitorAudit> history = new MonitorService().getMonitorHistory(monitorId, user.getOrganization());
            StringBuilder json = new StringBuilder("{\"history\":[");
            for (int i = 0; i < history.size(); i++) {
                MonitorAudit audit = history.get(i);
                if (i > 0) json.append(",");
                json.append("{");
                json.append("\"id\":").append(audit.getId()).append(",");
                json.append("\"monitor_id\":").append(audit.getMonitorId()).append(",");
                json.append("\"operation\":\"").append(escapeJson(audit.getOperation())).append("\",");
                json.append("\"time\":\"").append(new Timestamp(audit.getTime()).toString()).append("\"");
                json.append("}");
            }
            json.append("]}");
            return Response.ok(json.toString(), MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error fetching monitor history", e);
            return errorResponse(500, "Internal server error");
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static boolean isNullOrEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                     .replace("\"", "\\\"")
                     .replace("\n", "\\n")
                     .replace("\r", "\\r")
                     .replace("\t", "\\t");
    }

    private static Response errorResponse(int status, String message) {
        return Response.status(status)
                .entity("{\"error\":\"" + escapeJson(message) + "\"}")
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
