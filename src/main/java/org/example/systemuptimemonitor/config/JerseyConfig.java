package org.example.systemuptimemonitor.config;

import org.glassfish.jersey.server.ResourceConfig;
import javax.ws.rs.ApplicationPath;

@ApplicationPath("/api")
public class JerseyConfig extends ResourceConfig {
    public JerseyConfig() {
        packages("org.example.systemuptimemonitor.resources",
                 "org.example.systemuptimemonitor.filter");
    }
}
