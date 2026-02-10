package org.example.systemuptimemonitor.filter;

import org.example.systemuptimemonitor.dao.UserDao;
import org.example.systemuptimemonitor.exceptions.MissingUserException;
import org.example.systemuptimemonitor.model.User;
import org.example.systemuptimemonitor.util.ErrorResponse;
import org.example.systemuptimemonitor.util.TokenManager;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

@WebFilter({"/generate_invitelink", "/delete_user"})
public class AdminAuthenticationFilter extends HttpFilter {
    private static final Logger LOG = Logger.getLogger(AdminAuthenticationFilter.class.getName());

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for(Cookie cookie: cookies) {
                if (cookie.getName().equals("token") && TokenManager.isValid(cookie.getValue())) {
                    User user = TokenManager.getUser(cookie.getValue());
                    if (user.getRole().equals("admin")) {
                        request.getSession().setAttribute("user", user);
                        LOG.info("Admin access granted for user: " + user.getEmail() + " on " + request.getRequestURI());
                        chain.doFilter(request, response);
                        return;
                    }
                }
            }
        }

        LOG.warning("Admin access denied for " + request.getRequestURI() + " from " + request.getRemoteAddr());
        ErrorResponse.sendJsonError(response, HttpServletResponse.SC_FORBIDDEN, "Access denied");
    }
}
