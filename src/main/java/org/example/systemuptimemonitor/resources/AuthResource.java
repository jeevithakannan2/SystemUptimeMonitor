package org.example.systemuptimemonitor.resources;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.example.systemuptimemonitor.dao.InviteLinkDao;
import org.example.systemuptimemonitor.dao.UserDao;
import org.example.systemuptimemonitor.exceptions.InviteLinkExpiredException;
import org.example.systemuptimemonitor.exceptions.MissingUserException;
import org.example.systemuptimemonitor.exceptions.RoleMissingException;
import org.example.systemuptimemonitor.exceptions.UserAlreadyExistsException;
import org.example.systemuptimemonitor.model.InviteLink;
import org.example.systemuptimemonitor.model.User;
import org.example.systemuptimemonitor.services.UserService;
import org.example.systemuptimemonitor.util.DBManager;
import org.example.systemuptimemonitor.util.SchemaManager;
import org.example.systemuptimemonitor.util.TokenManager;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/")
public class AuthResource {
    private static final Logger LOG = Logger.getLogger(AuthResource.class.getName());

    @GET
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(@QueryParam("email") String email,
                          @QueryParam("password") String password) {
        if (email == null || password == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Email and password are required\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }

        String[] emailSplit = email.split("@");
        if (emailSplit.length != 2) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Not a valid email\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }
        String organization = emailSplit[1];

        try {
            if (!SchemaManager.organizationExists(organization)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("{\"error\":\"Organization not found\"}")
                        .type(MediaType.APPLICATION_JSON).build();
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to check organization: " + organization, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Server error\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }

        UserDao userDao = new UserDao();
        User user;
        try (Connection connection = DBManager.getConnection(organization)) {
            user = userDao.getUserByEmail(connection, email);
        } catch (MissingUserException e) {
            LOG.warning("Login failed - user not found: " + email);
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"error\":\"User not found\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Database error during login: " + email, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Server error\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }

        if (user == null) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"error\":\"User not found\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }

        if (!BCrypt.checkpw(password, user.getPassword())) {
            LOG.warning("Login failed - wrong password for: " + email);
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"error\":\"Wrong password\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }

        String token = TokenManager.createToken(user);
        NewCookie tokenCookie = new NewCookie("token", token, "/", null, null, 3600, false, true);

        LOG.info("User logged in: " + email + " (role=" + user.getRole() + ")");

        String json = "{\"role\":\"" + user.getRole()
                + "\",\"email\":\"" + user.getEmail()
                + "\",\"organization\":\"" + user.getOrganization()
                + "\",\"email_notifications\":" + user.isEmailNotifications() + "}";

        return Response.ok(json, MediaType.APPLICATION_JSON)
                .cookie(tokenCookie).build();
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(@FormParam("email") String email,
                             @FormParam("password") String password,
                             @FormParam("role") String role) {
        if (email == null || password == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Email and password are required\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }

        String[] emailSplit = email.split("@");
        if (emailSplit.length != 2) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Not a valid email\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }

        String organization = emailSplit[1];
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        User user = new User(email, hashedPassword, role, organization);
        UserService userService = new UserService();

        try {
            userService.createUser(user);
            LOG.info("User registered: " + email + " (org=" + organization + ")");
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to register user: " + email, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Cannot create user\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (RoleMissingException e) {
            LOG.warning("Registration failed - invalid role for: " + email);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Role parameter should be either viewer or operator\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }

        return Response.ok().build();
    }

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@FormParam("code") String code,
                           @FormParam("email") String email,
                           @FormParam("password") String password) {
        if (code == null || code.isEmpty()) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"error\":\"Invite code is required\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }

        String[] emailSplit = email.split("@");
        if (emailSplit.length != 2) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Not a valid email\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }

        String organization = emailSplit[1];

        try {
            if (!SchemaManager.organizationExists(organization)) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\":\"Organization not found\"}")
                        .type(MediaType.APPLICATION_JSON).build();
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to check organization: " + organization, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Server error\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }

        InviteLink inviteLink = null;
        try (Connection connection = DBManager.getConnection(organization)) {
            inviteLink = new InviteLinkDao().getInviteLink(connection, code);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to look up invite link: " + code, e);
        }

        if (inviteLink == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Invite link expired\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        User user = new User(email, hashedPassword, inviteLink.getRole(), organization);
        UserService userService = new UserService();

        try {
            userService.createUserFromLink(user, inviteLink, organization);
            LOG.info("User created via invite link: " + email + " (role=" + inviteLink.getRole() + ")");
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to create user from invite link: " + email, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Server error\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (InviteLinkExpiredException e) {
            LOG.warning("Invite link expired for user: " + email);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Invite link expired\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (UserAlreadyExistsException e) {
            LOG.warning("User already exists: " + email);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"User already exists\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }

        return Response.ok().build();
    }
}
