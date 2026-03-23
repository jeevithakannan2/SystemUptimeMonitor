package org.example.systemuptimemonitor.resources;

import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.example.systemuptimemonitor.dao.MonitorSubscriptionDao;
import org.example.systemuptimemonitor.dao.UserDao;
import org.example.systemuptimemonitor.filter.OperatorAuth;
import org.example.systemuptimemonitor.model.User;
import org.example.systemuptimemonitor.util.DBManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/")
@OperatorAuth
public class NotificationResource {
    private static final Logger LOG = Logger.getLogger(NotificationResource.class.getName());
    private static final UserDao userDao = new UserDao();
    private static final MonitorSubscriptionDao subscriptionDao = new MonitorSubscriptionDao();

    @PUT
    @Path("/notification_preference")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateNotificationPreference(@FormParam("enabled") String enabledStr,
                                                  @Context ContainerRequestContext crc) {
        User user = (User) crc.getProperty("user");
        if (enabledStr == null || enabledStr.isEmpty()) {
            return errorResponse(400, "enabled parameter is required");
        }
        boolean enabled = Boolean.parseBoolean(enabledStr);
        try (Connection conn = DBManager.getConnection(user.getOrganization())) {
            userDao.updateEmailNotifications(conn, user.getId(), enabled);
            LOG.info("Notification preference updated for user " + user.getEmail() + ": " + enabled);
            return Response.ok("{\"email_notifications\":" + enabled + "}", MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to update notification preference", e);
            return errorResponse(500, "Failed to update preference");
        }
    }

    @POST
    @Path("/subscribe_monitor")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response subscribeMonitor(@FormParam("monitor_id") String monitorIdStr,
                                     @Context ContainerRequestContext crc) {
        User user = (User) crc.getProperty("user");
        if (monitorIdStr == null || monitorIdStr.isEmpty()) {
            return errorResponse(400, "monitor_id is required");
        }
        int monitorId;
        try {
            monitorId = Integer.parseInt(monitorIdStr);
        } catch (NumberFormatException e) {
            return errorResponse(400, "Invalid monitor_id");
        }
        try (Connection conn = DBManager.getConnection(user.getOrganization())) {
            subscriptionDao.subscribe(conn, user.getId(), monitorId);
            LOG.info("User " + user.getEmail() + " subscribed to monitor " + monitorId);
            return Response.ok("{\"message\":\"Subscribed\"}", MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to subscribe to monitor " + monitorId, e);
            return errorResponse(500, "Failed to subscribe");
        }
    }

    @DELETE
    @Path("/unsubscribe_monitor")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response unsubscribeMonitor(@FormParam("monitor_id") String monitorIdStr,
                                       @Context ContainerRequestContext crc) {
        User user = (User) crc.getProperty("user");
        if (monitorIdStr == null || monitorIdStr.isEmpty()) {
            return errorResponse(400, "monitor_id is required");
        }
        int monitorId;
        try {
            monitorId = Integer.parseInt(monitorIdStr);
        } catch (NumberFormatException e) {
            return errorResponse(400, "Invalid monitor_id");
        }
        try (Connection conn = DBManager.getConnection(user.getOrganization())) {
            subscriptionDao.unsubscribe(conn, user.getId(), monitorId);
            LOG.info("User " + user.getEmail() + " unsubscribed from monitor " + monitorId);
            return Response.ok("{\"message\":\"Unsubscribed\"}", MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to unsubscribe from monitor " + monitorId, e);
            return errorResponse(500, "Failed to unsubscribe");
        }
    }

    @GET
    @Path("/subscriptions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSubscriptions(@Context ContainerRequestContext crc) {
        User user = (User) crc.getProperty("user");
        try (Connection conn = DBManager.getConnection(user.getOrganization())) {
            List<Integer> monitorIds = subscriptionDao.getSubscribedMonitorIds(conn, user.getId());
            User freshUser = userDao.getUserByEmail(conn, user.getEmail());
            boolean emailNotifications = freshUser != null ? freshUser.isEmailNotifications() : false;
            StringBuilder json = new StringBuilder("{\"email_notifications\":");
            json.append(emailNotifications);
            json.append(",\"subscribed_monitors\":[");
            for (int i = 0; i < monitorIds.size(); i++) {
                if (i > 0) json.append(",");
                json.append(monitorIds.get(i));
            }
            json.append("]}");
            return Response.ok(json.toString(), MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to get subscriptions", e);
            return errorResponse(500, "Failed to load subscriptions");
        }
    }

    private static Response errorResponse(int status, String message) {
        return Response.status(status)
                .entity("{\"error\":\"" + message + "\"}")
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
