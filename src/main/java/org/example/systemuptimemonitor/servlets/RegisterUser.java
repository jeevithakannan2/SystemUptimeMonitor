package org.example.systemuptimemonitor.servlets;

import org.example.systemuptimemonitor.exceptions.RoleMissingException;
import org.example.systemuptimemonitor.model.User;
import org.example.systemuptimemonitor.services.UserService;
import org.example.systemuptimemonitor.util.ErrorResponse;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "Register Servlet", value = "/register")
public class RegisterUser extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(RegisterUser.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String role = req.getParameter("role");

        if (email == null || password == null) {
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Email and password are required");
            return;
        }

        String[] emailSplit = email.split("@");
        if (emailSplit.length != 2) {
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Not a valid email");
            return;
        }

        String organization = emailSplit[1];
        String salt = BCrypt.gensalt();
        password = BCrypt.hashpw(password, salt);
        User user = new User(email, password, role, organization);
        UserService userService = new UserService();

        try {
            userService.createUser(user);
            LOG.info("User registered: " + email + " (org=" + organization + ")");
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to register user: " + email, e);
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Cannot create user");
        } catch (RoleMissingException e) {
            LOG.warning("Registration failed - invalid role for: " + email);
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Role parameter should be either viewer or operator");
        }
    }
}
