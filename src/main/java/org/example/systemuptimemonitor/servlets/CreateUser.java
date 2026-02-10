package org.example.systemuptimemonitor.servlets;

import org.example.systemuptimemonitor.dao.InviteLinkDao;
import org.example.systemuptimemonitor.exceptions.InviteLinkExpiredException;
import org.example.systemuptimemonitor.exceptions.UserAlreadyExistsException;
import org.example.systemuptimemonitor.model.InviteLink;
import org.example.systemuptimemonitor.model.User;
import org.example.systemuptimemonitor.services.UserService;
import org.mindrot.jbcrypt.BCrypt;

import org.example.systemuptimemonitor.util.ErrorResponse;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/create")
public class CreateUser extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(CreateUser.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String code = req.getParameter("code");
        String email = req.getParameter("email");
        String password = req.getParameter("password");

        if (code == null || code.isEmpty()) {
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_FORBIDDEN, "Invite code is required");
            return;
        }

        String[] emailSplit = email.split("@");
        if (emailSplit.length != 2) {
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Not a valid email");
            return;
        }

        String organization = emailSplit[1];
        InviteLink inviteLink = null;
        try {
            inviteLink = new InviteLinkDao().getInviteLink(code);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to look up invite link: " + code, e);
        }

        if (inviteLink == null) {
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invite link expired");
            return;
        }

        String salt = BCrypt.gensalt();
        password = BCrypt.hashpw(password, salt);
        User user = new User(email, password, inviteLink.getRole(), organization);
        UserService userService = new UserService();

        try {
            userService.createUserFromLink(user, inviteLink);
            LOG.info("User created via invite link: " + email + " (role=" + inviteLink.getRole() + ")");
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to create user from invite link: " + email, e);
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
        } catch (InviteLinkExpiredException e) {
            LOG.warning("Invite link expired for user: " + email);
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invite link expired");
        } catch (UserAlreadyExistsException e) {
            LOG.warning("User already exists: " + email);
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "User already exists");
        }
    }
}
