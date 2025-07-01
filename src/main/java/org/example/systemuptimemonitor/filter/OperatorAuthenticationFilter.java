package org.example.systemuptimemonitor.filter;

import org.example.systemuptimemonitor.dao.UserDao;
import org.example.systemuptimemonitor.exceptions.MissingUserException;
import org.example.systemuptimemonitor.model.User;
import org.example.systemuptimemonitor.util.TokenManager;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter({"/create_monitor", "/delete_monitor", "/update_monitor", "/monitors", "/incidents", "/create_incident", "/resolve_incident"})
public class OperatorAuthenticationFilter extends HttpFilter {
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("token") && TokenManager.isValid(cookie.getValue())) {
                    User user = TokenManager.getUser(cookie.getValue());
                    if (user.getRole().equals("operator")) {
                        request.getSession().setAttribute("user", user);
                        chain.doFilter(request, response);
                        return;
                    }
                }
            }
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
}
