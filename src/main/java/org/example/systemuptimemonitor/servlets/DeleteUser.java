package org.example.systemuptimemonitor.servlets;

import org.example.systemuptimemonitor.services.UserService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/delete_user")
public class DeleteUser extends HttpServlet {
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = req.getParameter("delete_email");

        if (email == null || email.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Not a valid email");
            return;
        }

        UserService userService = new UserService();
        try {
            userService.deleteUser(email);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
