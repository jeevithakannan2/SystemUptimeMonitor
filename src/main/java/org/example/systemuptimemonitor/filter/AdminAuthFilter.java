package org.example.systemuptimemonitor.filter;

import org.example.systemuptimemonitor.model.User;
import org.example.systemuptimemonitor.util.TokenManager;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.logging.Logger;

@Provider
@AdminAuth
public class AdminAuthFilter implements ContainerRequestFilter {
    private static final Logger LOG = Logger.getLogger(AdminAuthFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext ctx) {
        Cookie cookie = ctx.getCookies().get("token");
        if (cookie != null && TokenManager.isValid(cookie.getValue())) {
            User user = TokenManager.getUser(cookie.getValue());
            if (user != null && "admin".equals(user.getRole())) {
                ctx.setProperty("user", user);
                LOG.info("Admin access granted for " + user.getEmail());
                return;
            }
        }
        LOG.warning("Admin access denied for " + ctx.getUriInfo().getPath());
        ctx.abortWith(Response.status(Response.Status.FORBIDDEN)
                .entity("{\"error\":\"Access denied\"}")
                .type("application/json")
                .build());
    }
}
