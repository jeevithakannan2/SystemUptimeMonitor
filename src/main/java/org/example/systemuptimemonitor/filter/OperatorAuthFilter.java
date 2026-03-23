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
@OperatorAuth
public class OperatorAuthFilter implements ContainerRequestFilter {
    private static final Logger LOG = Logger.getLogger(OperatorAuthFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext ctx) {
        Cookie cookie = ctx.getCookies().get("token");
        if (cookie != null && TokenManager.isValid(cookie.getValue())) {
            User user = TokenManager.getUser(cookie.getValue());
            if (user != null && ("operator".equals(user.getRole()) || "admin".equals(user.getRole()))) {
                ctx.setProperty("user", user);
                LOG.fine("Operator access granted for " + user.getEmail());
                return;
            }
        }
        LOG.warning("Operator access denied for " + ctx.getUriInfo().getPath());
        ctx.abortWith(Response.status(Response.Status.FORBIDDEN)
                .entity("{\"error\":\"Access denied\"}")
                .type("application/json")
                .build());
    }
}
