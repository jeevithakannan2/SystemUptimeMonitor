package org.example.systemuptimemonitor.servlets;

import org.example.systemuptimemonitor.exceptions.RoleMissingException;
import org.example.systemuptimemonitor.model.User;
import org.example.systemuptimemonitor.services.UserService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet(name = "Register Servlet", value = "/register")
public class RegisterUser extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String role = req.getParameter("role");

        if (email == null || password == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "email, password fields req");
            return;
        }

        String[] emailSplit = email.split("@");
        if (emailSplit.length != 2) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Not a valid email");
            return;
        }

        String organization = emailSplit[1];
        User user = new User(email, password, role, organization);
        UserService userService = new UserService();

        try {
            userService.createUser(user);
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Cannot create user");
        } catch (RoleMissingException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Role parameter should be either viewer or operator");
        }
    }
}
