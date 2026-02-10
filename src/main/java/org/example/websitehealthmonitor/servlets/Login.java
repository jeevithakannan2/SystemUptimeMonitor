package org.example.websitehealthmonitor.servlets;

import org.example.websitehealthmonitor.dao.UserDao;
import org.example.websitehealthmonitor.exceptions.MissingUserException;
import org.example.websitehealthmonitor.model.User;
import org.example.websitehealthmonitor.util.DBManager;
import org.example.websitehealthmonitor.util.ErrorResponse;
import org.example.websitehealthmonitor.util.SchemaManager;
import org.example.websitehealthmonitor.util.TokenManager;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/login")
public class Login extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(Login.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = req.getParameter("email");
        String password = req.getParameter("password");

        if (email == null || password == null) {
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Email and password are required");
            return;
        }

        // Derive organization from email domain
        String[] emailSplit = email.split("@");
        if (emailSplit.length != 2) {
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Not a valid email");
            return;
        }
        String organization = emailSplit[1];

        // Check org exists
        try {
            if (!SchemaManager.organizationExists(organization)) {
                ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_FORBIDDEN, "Organization not found");
                return;
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to check organization: " + organization, e);
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
            return;
        }

        UserDao userDao = new UserDao();
        User user = null;
        try (Connection connection = DBManager.getConnection(organization)) {
            user = userDao.getUserByEmail(connection, email);
        } catch (MissingUserException e) {
            LOG.warning("Login failed - user not found: " + email);
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_FORBIDDEN, "User not found");
            return;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Database error during login: " + email, e);
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
            return;
        }

        if (user == null) {
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_FORBIDDEN, "User not found");
            return;
        }

        if (!BCrypt.checkpw(password, user.getPassword())) {
            LOG.warning("Login failed - wrong password for: " + email);
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_FORBIDDEN, "Wrong password");
            return;
        }

        String token = TokenManager.createToken(user);
        Cookie tokenCookie = new Cookie("token", token);
        tokenCookie.setHttpOnly(true);
        tokenCookie.setSecure(true);
        tokenCookie.setMaxAge(3600);
        resp.addCookie(tokenCookie);

        LOG.info("User logged in: " + email + " (role=" + user.getRole() + ")");

        resp.setContentType("application/json");
        resp.getWriter().print("{\"role\":\"" + user.getRole() + "\",\"email\":\"" + user.getEmail() + "\",\"organization\":\"" + user.getOrganization() + "\"}");
    }
}
