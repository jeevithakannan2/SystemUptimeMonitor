package org.example.systemuptimemonitor.servlets;

import org.example.systemuptimemonitor.model.InviteLink;
import org.example.systemuptimemonitor.model.User;
import org.example.systemuptimemonitor.services.InviteLinkService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

@WebServlet("/generate_invitelink")
public class GenerateInviteLink extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = (User) req.getSession().getAttribute("user");
        String role = req.getParameter("role");
        if (role == null || !role.equals("operator") && !role.equals("viewer")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Roles should either operator or viewer");
            return;
        }

        String url = "" + System.currentTimeMillis();
        InviteLink inviteLink = new InviteLink(user.getId(), System.currentTimeMillis(), false, url, role);
        InviteLinkService inviteLinkService = new InviteLinkService();

        try {
            inviteLinkService.createInviteLink(inviteLink);
        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        resp.setContentType("text/plain");
        PrintWriter pw = resp.getWriter();
        pw.println(inviteLink.getUrl());
        pw.close();
    }
}
