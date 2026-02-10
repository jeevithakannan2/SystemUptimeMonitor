package org.example.websitehealthmonitor.servlets;

import org.example.websitehealthmonitor.model.User;
import org.example.websitehealthmonitor.services.UserService;
import org.example.websitehealthmonitor.util.ErrorResponse;
import org.example.websitehealthmonitor.util.RequestBodyParser;

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

@WebServlet("/delete_user")
public class DeleteUser extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(DeleteUser.class.getName());

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = (User) req.getSession().getAttribute("user");
        Map<String, String> params = RequestBodyParser.parse(req);
        String email = params.get("delete_email");

        if (email == null || email.isEmpty()) {
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Not a valid email");
            return;
        }

        UserService userService = new UserService();
        try {
            userService.deleteUser(email, user.getOrganization());
            LOG.info("User deleted: " + email);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to delete user: " + email, e);
            ErrorResponse.sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to delete user");
        }
    }
}
