package org.example.systemuptimemonitor.servlets;

import org.example.systemuptimemonitor.dao.InviteLinkDao;
import org.example.systemuptimemonitor.exceptions.InviteLinkExpiredException;
import org.example.systemuptimemonitor.exceptions.UserAlreadyExistsException;
import org.example.systemuptimemonitor.model.InviteLink;
import org.example.systemuptimemonitor.model.User;
import org.example.systemuptimemonitor.services.UserService;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/create")
public class CreateUser extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String code = req.getParameter("code");
        String email = req.getParameter("email");
        String password = req.getParameter("password");

        if (code == null || code.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String[] emailSplit = email.split("@");
        if (emailSplit.length != 2) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Not a valid email");
            return;
        }

        String organization = emailSplit[1];
        InviteLink inviteLink = null;
        try {
            inviteLink = new InviteLinkDao().getInviteLink(code);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (inviteLink == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invite link expired");
            return;
        }

        String salt = BCrypt.gensalt();
        password = BCrypt.hashpw(password, salt);
        User user = new User(email, password, inviteLink.getRole(), organization);
        UserService userService = new UserService();

        try {
            userService.createUserFromLink(user, inviteLink);
        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (InviteLinkExpiredException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invite link expired");
        } catch (UserAlreadyExistsException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "User already exists");
        }
    }
}
