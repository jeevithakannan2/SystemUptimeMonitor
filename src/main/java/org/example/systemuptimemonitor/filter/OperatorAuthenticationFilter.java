package org.example.systemuptimemonitor.filter;

import org.example.systemuptimemonitor.dao.UserDao;
import org.example.systemuptimemonitor.exceptions.MissingUserException;
import org.example.systemuptimemonitor.model.User;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter({"/create_monitor"})
public class OperatorAuthenticationFilter extends HttpFilter {
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        UserDao userDao = new UserDao();
        User user = null;
        try {
            user = userDao.getUserByEmail(email);
        } catch (MissingUserException e) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (user == null || !user.getRole().equals("operator")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (!user.getPassword().equals(password)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Wrong password");
            return;
        }

        request.getSession().setAttribute("user", user);
        chain.doFilter(request, response);
    }
}
