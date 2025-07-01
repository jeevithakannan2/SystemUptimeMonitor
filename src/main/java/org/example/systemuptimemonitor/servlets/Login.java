package org.example.systemuptimemonitor.servlets;

import org.example.systemuptimemonitor.dao.UserDao;
import org.example.systemuptimemonitor.exceptions.MissingUserException;
import org.example.systemuptimemonitor.model.User;
import org.example.systemuptimemonitor.util.TokenManager;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/login")
public class Login extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = req.getParameter("email");
        String password = req.getParameter("password");

        UserDao userDao = new UserDao();
        User user = null;
        try {
            user = userDao.getUserByEmail(email);
        } catch (MissingUserException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (!BCrypt.checkpw(password, user.getPassword())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Wrong password");
            return;
        }

        String token = TokenManager.createToken(user);
        Cookie tokenCookie = new Cookie("token", token);
        tokenCookie.setHttpOnly(true);
        tokenCookie.setSecure(true);
        tokenCookie.setMaxAge(3600);
        resp.addCookie(tokenCookie);
    }
}
